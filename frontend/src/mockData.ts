import type { Match } from './types'

export const mockMatches: Match[] = [
  {
    id: 'ind-aus-2026', status: 'LIVE', format: '3rd ODI · 50 overs', venue: 'Narendra Modi Stadium, Ahmedabad',
    series: 'AUSTRALIA TOUR OF INDIA',
    homeTeam: { name: 'India', code: 'IND', flag: '🇮🇳', runs: 287, wickets: 4, overs: '43.2' },
    awayTeam: { name: 'Australia', code: 'AUS', flag: '🇦🇺', runs: 0, wickets: 0, overs: '0.0' },
    battingTeam: 'IND', target: 0, runRate: 6.62, requiredRunRate: 0, toss: 'India elected to bat',
    batterOne: { name: 'Virat Kohli', runs: 86, balls: 74, fours: 8, sixes: 2 },
    batterTwo: { name: 'KL Rahul', runs: 42, balls: 35, fours: 3, sixes: 1 },
    bowler: { name: 'Josh Hazlewood', runs: 48, balls: 0, fours: 0, sixes: 0, economy: 5.14, wickets: 1, overs: '9.2' },
    recentBalls: ['1', '4', '1', '6', '0', '2'], updatedAt: 'Just now',
    commentary: [
      { over: '43.2', text: 'Kohli opens the face and guides it past point for a single. A measured knock, inching toward a hundred.', kind: 'normal', time: '12:42 PM' },
      { over: '43.1', text: 'Rahul gets hold of that one! Sails over deep midwicket for SIX.', kind: 'boundary', time: '12:41 PM' },
      { over: '42.6', text: 'A tidy over from Hazlewood. Just five from it.', kind: 'normal', time: '12:41 PM' },
      { over: '42.4', text: 'Four! Kohli leans into the drive and beats extra cover.', kind: 'boundary', time: '12:40 PM' },
      { over: '41.3', text: 'Gone! Iyer picks out the fielder at long-on. India lose their fourth.', kind: 'wicket', time: '12:38 PM' },
    ],
  },
  {
    id: 'eng-sa-2026', status: 'LIVE', format: '1st Test · Day 2', venue: 'Lord’s, London',
    series: 'ENGLAND V SOUTH AFRICA',
    homeTeam: { name: 'England', code: 'ENG', flag: '🏴', runs: 196, wickets: 6, overs: '58.4' },
    awayTeam: { name: 'South Africa', code: 'SA', flag: '🇿🇦', runs: 312, wickets: 10, overs: '92.1' },
    battingTeam: 'ENG', target: 0, runRate: 3.34, requiredRunRate: 0, toss: 'South Africa elected to bat',
    batterOne: { name: 'Ben Stokes', runs: 38, balls: 72, fours: 4, sixes: 0 },
    batterTwo: { name: 'Jamie Smith', runs: 21, balls: 34, fours: 2, sixes: 0 },
    bowler: { name: 'Kagiso Rabada', runs: 42, balls: 0, fours: 0, sixes: 0, economy: 2.8, wickets: 2, overs: '15.0' },
    recentBalls: ['0', '1', '0', '0', '4', '0'], updatedAt: '2 min ago',
    commentary: [
      { over: '58.4', text: 'Stokes nudges it into the leg side for one. England are 116 behind.', kind: 'normal', time: '12:40 PM' },
      { over: '58.1', text: 'Four! Smith finds the gap through the covers.', kind: 'boundary', time: '12:39 PM' },
      { over: '56.5', text: 'Big appeal and given! Root has to go, trapped in front by Rabada.', kind: 'wicket', time: '12:34 PM' },
    ],
  },
  {
    id: 'pak-wi-2026', status: 'UPCOMING', format: '2nd T20I · 7:30 PM', venue: 'Gaddafi Stadium, Lahore',
    series: 'WEST INDIES TOUR OF PAKISTAN',
    homeTeam: { name: 'Pakistan', code: 'PAK', flag: '🇵🇰', runs: 0, wickets: 0, overs: '0.0' },
    awayTeam: { name: 'West Indies', code: 'WI', flag: '🏴‍☠️', runs: 0, wickets: 0, overs: '0.0' },
    battingTeam: 'PAK', target: 0, runRate: 0, requiredRunRate: 0, toss: 'Match starts in 02:14:36',
    batterOne: { name: 'Match preview', runs: 0, balls: 0, fours: 0, sixes: 0 },
    batterTwo: { name: '', runs: 0, balls: 0, fours: 0, sixes: 0 },
    bowler: { name: '', runs: 0, balls: 0, fours: 0, sixes: 0, economy: 0, wickets: 0, overs: '0.0' },
    recentBalls: [], updatedAt: 'Today', commentary: [],
  },
]
