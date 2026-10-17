import { Fragment, useEffect, useMemo, useState } from 'react'
import {
  Activity, ArrowRight, Bell, CalendarDays, Check, ChevronDown, CircleHelp,
  Clock3, Flame, LayoutDashboard, Menu, MoreHorizontal, Radio, Search,
  Settings2, Shield, Sparkles, Trophy, X,
} from 'lucide-react'
import { connectToMatchFeed, fetchMatches, fetchScorecard } from './api'
import { mockMatches } from './mockData'
import type { Match, Player, Scorecard } from './types'

function PlayerRow({ player, active = false }: { player: Player; active?: boolean }) {
  return <div className={`player-row${active ? ' active-player' : ''}`}>
    <span className="player-name">{player.name}{active && <i>✦</i>}</span>
    <b>{player.runs}<small> ({player.balls})</small></b>
    <span>{player.fours}<small>4s</small>　{player.sixes}<small>6s</small></span>
  </div>
}
function App() {
  const [matches, setMatches] = useState<Match[]>(mockMatches)
  const [selectedId, setSelectedId] = useState(mockMatches[0].id)
  const [filter, setFilter] = useState<'ALL' | 'LIVE' | 'UPCOMING'>('ALL')
  const [connected, setConnected] = useState(false)
  const [mobileNav, setMobileNav] = useState(false)
  const [toast, setToast] = useState('')
  const [modal, setModal] = useState<'scorecard' | 'commentary' | null>(null)
  const [scorecard, setScorecard] = useState<Scorecard | null>(null)
  const [scorecardNotice, setScorecardNotice] = useState('')

  useEffect(() => {
    let mounted = true
    fetchMatches().then((data) => {
      if (mounted && data.length) {
        setMatches(data)
        setSelectedId(data[0].id)
        setConnected(true)
      }
    }).catch(() => { if (mounted) setConnected(false) })
    const disconnect = connectToMatchFeed((data) => {
      if (mounted && data.length) {
        setMatches(data)
        setConnected(true)
      }
    })
    const poll = window.setInterval(() => {
      fetchMatches().then((data) => {
        if (mounted && data.length) {
          setMatches(data)
          setConnected(true)
        }
      }).catch(() => { if (mounted) setConnected(false) })
    }, 5000)
    return () => { mounted = false; disconnect(); window.clearInterval(poll) }
  }, [])

  const selected = matches.find((match) => match.id === selectedId) ?? matches[0]
  const visibleMatches = useMemo(() => matches.filter((match) => filter === 'ALL' || match.status === filter), [filter, matches])
  if (!selected) return null
  const batting = selected.battingTeam === selected.homeTeam.code ? selected.homeTeam : selected.awayTeam
  const bowling = batting.code === selected.homeTeam.code ? selected.awayTeam : selected.homeTeam
  const todayRuns = matches.reduce((total, match) => total + match.homeTeam.runs + match.awayTeam.runs, 0)
  const todayWickets = matches.reduce((total, match) => total + match.homeTeam.wickets + match.awayTeam.wickets, 0)
  const notify = (message: string) => {
    setToast(message)
    window.setTimeout(() => setToast(''), 2600)
  }
  const showScorecard = async () => {
    setModal('scorecard')
    setScorecard(null)
    setScorecardNotice('')
    try {
      setScorecard(await fetchScorecard(selected.id))
    } catch (error) {
      setScorecard({
        matchId: selected.id,
        battingTeam: batting.name,
        batters: [selected.batterOne, selected.batterTwo].filter((player) => player.name).map((player) => ({
          name: player.name, runs: player.runs, balls: player.balls, fours: player.fours, sixes: player.sixes, dismissal: 'not out',
        })),
        bowlers: selected.bowler.name ? [{
          name: selected.bowler.name, overs: selected.bowler.overs ?? '0.0', maidens: 0,
          runs: selected.bowler.runs, wickets: selected.bowler.wickets ?? 0, economy: selected.bowler.economy ?? 0,
        }] : [],
      })
      setScorecardNotice(error instanceof Error ? `${error.message}. Showing demo match stats.` : 'Scorecard unavailable. Showing demo match stats.')
    }
  }

  return <div className="app-shell">
    <aside className={`sidebar${mobileNav ? ' open' : ''}`}>
      <div className="brand"><div className="brand-mark"><span /></div><span>boundary<span className="brand-period">.</span></span><button className="mobile-close" onClick={() => setMobileNav(false)}><X size={18} /></button></div>
      <div className="workspace-label">WORKSPACE <button onClick={() => notify('Workspace options')}><MoreHorizontal size={16} /></button></div>
      <nav className="main-nav">
        <button className="nav-item active"><LayoutDashboard size={18} /><span>Overview</span><span className="nav-pill">{matches.length}</span></button>
        <button className="nav-item" onClick={() => notify('Match center is up to date')}><Activity size={18} /><span>Match center</span></button>
        <button className="nav-item" onClick={() => notify('No saved tournaments yet')}><Trophy size={18} /><span>Tournaments</span></button>
        <button className="nav-item" onClick={() => notify('Schedule synced for today')}><CalendarDays size={18} /><span>Schedule</span></button>
      </nav>
      <div className="sidebar-divider" />
      <div className="workspace-label">YOUR TEAMS <button onClick={() => notify('Team management coming soon')}><span className="plus">+</span></button></div>
      <nav className="team-nav">
        <button className="team-link"><span className="team-dot">🇮🇳</span>India<span className="team-live-dot" /></button>
        <button className="team-link"><span className="team-dot">🇦🇺</span>Australia</button>
        <button className="team-link"><span className="team-dot">🏴</span>England</button>
        <button className="add-team" onClick={() => notify('Team management coming soon')}><span>+</span> Add a team</button>
      </nav>
      <div className="sidebar-bottom">
        <div className="upgrade-card"><div className="upgrade-icon"><Sparkles size={15} /></div><strong>Cricket, without<br />the noise.</strong><p>Get the full picture with Boundary Pro.</p><button onClick={() => notify('You’re on the list!')}>Explore Pro <ArrowRight size={14} /></button><div className="upgrade-ball" /></div>
        <button className="nav-item bottom-link" onClick={() => notify('Help center opened')}><CircleHelp size={17} /><span>Help center</span></button>
        <button className="profile-row" onClick={() => notify('Account settings')}><div className="avatar">AK</div><span className="profile-copy"><b>Arjun Kumar</b><small>Free plan</small></span><MoreHorizontal size={18} /></button>
      </div>
    </aside>
    {mobileNav && <button className="nav-scrim" aria-label="Close navigation" onClick={() => setMobileNav(false)} />}
    <main className="main-content">
      <header className="topbar">
        <button className="menu-button" onClick={() => setMobileNav(true)} aria-label="Open navigation"><Menu size={21} /></button>
        <div className="breadcrumbs"><span>Workspace</span><span className="crumb-slash">/</span><b>Overview</b></div>
        <div className="top-actions"><div className="search-box"><Search size={16} /><span>Search matches...</span><kbd>⌘ K</kbd></div><button className="icon-button" aria-label="Notifications" onClick={() => notify('You’re all caught up')}><Bell size={18} /><i /></button><div className="top-avatar">AK</div></div>
      </header>
      <div className="page-wrap">
        <section className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> SATURDAY, 17 OCTOBER 2026</div><h1>The game is <span>on.</span></h1><p>Your cricket world, all in one place.</p></div><button className="date-button" onClick={() => notify('Showing matches for today')}><CalendarDays size={16} /> Today <ChevronDown size={15} /></button></section>

        <section className="stats-row" aria-label="Match statistics">
          <div className="stat-card"><div className="stat-icon live-icon"><Radio size={16} /></div><div className="stat-label">LIVE MATCHES</div><div className="stat-value">{matches.filter((m) => m.status === 'LIVE').length}<span className="stat-change"><i /> happening now</span></div><div className="stat-foot">Across all competitions <ArrowRight size={13} /></div></div>
          <div className="stat-card"><div className="stat-icon runs-icon"><Activity size={16} /></div><div className="stat-label">RUNS SCORED TODAY</div><div className="stat-value">{todayRuns.toLocaleString()}</div><div className="stat-foot">Across {matches.length} fixtures <ArrowRight size={13} /></div></div>
          <div className="stat-card"><div className="stat-icon wicket-icon"><Shield size={16} /></div><div className="stat-label">WICKETS FALLEN</div><div className="stat-value">{todayWickets}<span className="stat-context">in {matches.length} fixtures</span></div><div className="stat-foot">Across today’s matches <ArrowRight size={13} /></div></div>
          <div className="stat-card form-card"><div className="stat-icon form-icon"><Flame size={16} /></div><div className="stat-label">MOMENTUM</div><div className="form-spark">{Array.from({ length: 15 }, (_, i) => <span key={i} />)}</div><div className="stat-foot">India on top <b className="momentum-dots">●●●○○</b></div></div>
        </section>

        <section className="content-grid">
          <div className="live-panel panel">
            <div className="section-header"><div><div className="section-title">In play <span className="count-badge">{matches.filter((m) => m.status === 'LIVE').length}</span></div><p>Follow every ball, as it happens.</p></div><button className="text-action" onClick={() => setFilter('ALL')}>All matches <ArrowRight size={15} /></button></div>
            <div className="match-tabs">{(['ALL', 'LIVE', 'UPCOMING'] as const).map((item) => <button key={item} className={filter === item ? 'chosen' : ''} onClick={() => setFilter(item)}>{item === 'ALL' ? 'All' : item === 'LIVE' ? 'Live now' : 'Upcoming'}{item === 'LIVE' && <i />}</button>)}<button className="filter-button" aria-label="More filters" onClick={() => notify('All formats selected')}><Settings2 size={16} /></button></div>
            <div className="match-list">{visibleMatches.map((match) => {
              const team = match.battingTeam === match.homeTeam.code ? match.homeTeam : match.awayTeam
              return <button key={match.id} className={`match-card${selected.id === match.id ? ' selected' : ''}`} onClick={() => setSelectedId(match.id)}>
                <div className="match-card-top"><span className="series-name">{match.series}</span>{match.status === 'LIVE' ? <span className="live-tag"><i />LIVE</span> : <span className="upcoming-tag">UP NEXT</span>}</div>
                <div className="match-teams">{[match.homeTeam, match.awayTeam].map((side) => <div className="match-team" key={side.code}><span className="flag">{side.flag}</span><span>{side.name}</span><b>{side.runs ? `${side.runs}/${side.wickets}` : '—'}</b></div>)}</div>
                <div className="match-card-foot"><span>{match.status === 'LIVE' ? `${team.code} batting · ${team.overs} ov` : match.format}</span><span>{match.status === 'LIVE' ? `${match.runRate.toFixed(2)} RR` : '7:30 PM'}</span></div>{selected.id === match.id && <div className="selected-line" />}
              </button>
            })}{visibleMatches.length === 0 && <div className="empty-matches">No matches in this view.</div>}</div>
            <div className="panel-footer"><span><i className="online-dot" /> Scores update automatically</span><button onClick={() => fetchMatches().then(setMatches).catch(() => notify('Using demo scores'))}>Refresh <span>↻</span></button></div>
          </div>

          <div className="score-panel panel">
            <div className="score-topline"><div className="live-label"><i />{selected.status === 'LIVE' ? 'LIVE' : selected.status}</div><button className="more-button" aria-label="Match options" onClick={() => notify('Match options')}><MoreHorizontal size={18} /></button></div>
            <div className="score-match-title">{selected.series}</div><div className="score-match-sub">{selected.format} <span>·</span> {selected.venue}</div>
            <div className="scoreboard">{[batting, bowling].map((side, i) => <div className={`score-team-row${i ? ' secondary' : ''}`} key={side.code}><div className="score-team-info"><span className="score-flag">{side.flag}</span><div><strong>{side.name}</strong><small>{side.code}</small></div></div><div className={i ? 'secondary-score' : 'big-score'}>{side.runs ? `${side.runs}/${side.wickets}` : i ? 'Yet to bat' : '—'}{side.runs > 0 && <small>({side.overs} ov)</small>}</div></div>)}</div>
            <div className="match-result"><span>{selected.status === 'LIVE' ? selected.toss : 'Match starts at 7:30 PM'}</span><span className="rr-chip"><Activity size={12} /> RR <b>{selected.runRate.toFixed(2)}</b></span></div>
            <div className="score-details"><div><span>CRR</span><b>{selected.runRate.toFixed(2)}</b></div><div><span>PARTNERSHIP</span><b>{selected.batterOne.runs + selected.batterTwo.runs} <small>({selected.batterOne.balls + selected.batterTwo.balls})</small></b></div><div><span>LAST 6 BALLS</span><div className="ball-list">{selected.recentBalls.map((ball, index) => <i key={`${ball}-${index}`} className={ball === '4' || ball === '6' ? 'boundary-ball' : ''}>{ball}</i>)}</div></div></div>
            <div className="scorecard-link"><button onClick={() => void showScorecard()}>Full scorecard <ArrowRight size={15} /></button><span><Clock3 size={12} /> Updated {selected.updatedAt}</span></div>
          </div>
        </section>

        <section className="bottom-grid">
          <div className="innings-panel panel"><div className="section-header compact"><div><div className="section-title">At the crease <span className="mini-live"><i /> LIVE</span></div><p>{batting.code} batting · {bowling.code} bowling</p></div><button className="more-button" aria-label="Player options" onClick={() => notify('Player stats opened')}><MoreHorizontal size={18} /></button></div>
            <div className="players-head"><span>BATTERS</span><span>R (B)</span><span>4s　6s</span></div><PlayerRow player={selected.batterOne} active /><PlayerRow player={selected.batterTwo} />
            <div className="bowler-row"><div className="bowler-avatar">{bowling.code.slice(0, 2)}</div><div className="bowler-copy"><span>NOW BOWLING</span><b>{selected.bowler.name || 'To be announced'}</b></div><div className="bowler-figures">{selected.bowler.overs}<small> ov</small>　{selected.bowler.runs}-{selected.bowler.wickets}</div><div className="economy"><small>ECON</small><b>{selected.bowler.economy?.toFixed(2) ?? '—'}</b></div></div>
          </div>
          <div className="commentary-panel panel"><div className="section-header compact"><div><div className="section-title">Ball by ball <span className="commentary-live"><i /> LIVE</span></div><p>Every moment, captured.</p></div><button className="text-action" onClick={() => setModal('commentary')}>Full commentary <ArrowRight size={15} /></button></div>
            <div className="commentary-list">{(selected.commentary.length ? selected.commentary.slice(0, 3) : [{ over: '—', text: 'Commentary will appear here when the match begins.', kind: 'normal' as const, time: '' }]).map((item, index) => <div className="commentary-item" key={`${item.over}-${index}`}><span className={`over-ball ${item.kind}`}>{item.over}</span><p>{item.text}</p>{index === 0 && <span className="latest-label">LATEST</span>}</div>)}</div>
          </div>
        </section>
        <footer className="page-footer"><span>© 2026 Boundary Cricket</span><span><span className="connected-indicator"><i className={connected ? 'connected' : ''} />{connected ? 'Live data connected' : 'Demo mode · Connect API for live scores'}</span><span>Data refreshes every 5 seconds</span></span></footer>
      </div>
    </main>
    {toast && <div className="toast"><Check size={16} />{toast}</div>}
    {modal && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setModal(null) }}>
      <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <header><div><span className="modal-eyebrow">{selected.series}</span><h2 id="detail-title">{modal === 'scorecard' ? `${scorecard?.battingTeam ?? batting.name} scorecard` : 'Ball-by-ball commentary'}</h2></div><button aria-label="Close" onClick={() => setModal(null)}><X size={18} /></button></header>
        {modal === 'scorecard' ? <div className="modal-body">
          {scorecardNotice && <p className="scorecard-notice">{scorecardNotice}</p>}
          <h3>Batting</h3><div className="detail-table detail-batters"><span>BATTER</span><span>R</span><span>B</span><span>4s</span><span>6s</span><span>DISMISSAL</span>
            {scorecard?.batters.map((player) => <Fragment key={player.name}><b>{player.name}</b><b>{player.runs}</b><span>{player.balls}</span><span>{player.fours}</span><span>{player.sixes}</span><span>{player.dismissal}</span></Fragment>)}
          </div>
          <h3>Bowling</h3><div className="detail-table detail-bowlers"><span>BOWLER</span><span>O</span><span>M</span><span>R</span><span>W</span><span>ECON</span>
            {scorecard?.bowlers.map((player) => <Fragment key={player.name}><b>{player.name}</b><span>{player.overs}</span><span>{player.maidens}</span><span>{player.runs}</span><b>{player.wickets}</b><span>{player.economy.toFixed(2)}</span></Fragment>)}
          </div>
        </div> : <div className="modal-body full-commentary">{selected.commentary.map((item, index) => <div className="commentary-item" key={`${item.over}-${index}`}><span className={`over-ball ${item.kind}`}>{item.over}</span><p>{item.text}</p><span className="commentary-time">{item.time}</span></div>)}{!selected.commentary.length && <p className="scorecard-notice">Commentary will appear here when the match begins.</p>}</div>}
      </section>
    </div>}
  </div>
}

export default App
