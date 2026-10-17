export type Team = {
  name: string
  code: string
  flag: string
  runs: number
  wickets: number
  overs: string
}

export type Player = {
  name: string
  runs: number
  balls: number
  fours: number
  sixes: number
  economy?: number
  wickets?: number
  overs?: string
}

export type Commentary = {
  over: string
  text: string
  kind: 'boundary' | 'wicket' | 'normal'
  time: string
}

export type Scorecard = {
  matchId: string
  battingTeam: string
  batters: Array<{
    name: string
    runs: number
    balls: number
    fours: number
    sixes: number
    dismissal: string
  }>
  bowlers: Array<{
    name: string
    overs: string
    maidens: number
    runs: number
    wickets: number
    economy: number
  }>
}

export type Match = {
  id: string
  status: 'LIVE' | 'UPCOMING' | 'COMPLETED'
  format: string
  venue: string
  series: string
  homeTeam: Team
  awayTeam: Team
  battingTeam: string
  target: number
  runRate: number
  requiredRunRate: number
  toss: string
  batterOne: Player
  batterTwo: Player
  bowler: Player
  recentBalls: string[]
  updatedAt: string
  commentary: Commentary[]
}