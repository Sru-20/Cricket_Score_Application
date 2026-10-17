import type { Match, Scorecard } from './types'
import { Client } from '@stomp/stompjs'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export async function fetchMatches(): Promise<Match[]> {
  const response = await fetch(`${API_URL}/matches`, { signal: AbortSignal.timeout(2500) })
  if (!response.ok) throw new Error(`Score service returned ${response.status}`)
  return response.json() as Promise<Match[]>
}

export async function fetchScorecard(matchId: string): Promise<Scorecard> {
  const response = await fetch(`${API_URL}/matches/${encodeURIComponent(matchId)}/scorecard`, { signal: AbortSignal.timeout(2500) })
  if (!response.ok) throw new Error(`Scorecard service returned ${response.status}`)
  return response.json() as Promise<Scorecard>
}

export function connectToMatchFeed(onUpdate: (matches: Match[]) => void): () => void {
  const socketUrl = API_URL.replace(/^http/, 'ws').replace(/\/api$/, '') + '/ws'
  const client = new Client({
    brokerURL: socketUrl,
    reconnectDelay: 5000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    onConnect: () => client.subscribe('/topic/matches', (message) => {
      try {
        onUpdate(JSON.parse(message.body) as Match[])
      } catch (error) {
        console.error('Ignoring invalid match-feed update', error)
      }
    }),
  })
  client.activate()
  return () => { void client.deactivate() }
}
