"use client";

import { FilmReel, MagnifyingGlass, Plus, ArrowRight, Heart, BookmarkSimple, Check, TelevisionSimple, X, SignOut, UserCircle, Trash } from "@phosphor-icons/react";
import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { api, Account, CatalogueCategory, CatalogueTitle, Dashboard, Media, WatchStatus } from "../lib/api";

const categories: { value: CatalogueCategory; label: string; provider: string }[] = [
  { value: "MOVIE", label: "Movies", provider: "TMDB" },
  { value: "TV_SERIES", label: "TV Series", provider: "TMDB" },
  { value: "ANIME_MOVIE", label: "Anime Movies", provider: "ANILIST" },
  { value: "ANIME_SERIES", label: "Anime Series", provider: "ANILIST" },
];
const discoverySections: { key: string; title: string; category: CatalogueCategory }[] = [
  { key: "movies", title: "Popular movies", category: "MOVIE" },
  { key: "series", title: "Series to explore", category: "TV_SERIES" },
  { key: "anime", title: "Anime highlights", category: "ANIME_SERIES" },
];
const statuses: WatchStatus[] = ["PLAN_TO_WATCH", "WATCHING", "COMPLETED", "ON_HOLD", "DROPPED"];
const blankDashboard: Dashboard = { featured: null, stats: { totalTitles: 0, movieCount: 0, seriesCount: 0, completedCount: 0, favouriteCount: 0 }, continueWatching: [], recentlyAdded: [] };

export default function Home() {
  const [view, setView] = useState<"discover" | "library">("discover");
  const [category, setCategory] = useState<CatalogueCategory>("MOVIE");
  const [query, setQuery] = useState("");
  const [appliedQuery, setAppliedQuery] = useState("");
  const [page, setPage] = useState(1);
  const [catalogue, setCatalogue] = useState<CatalogueTitle[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [library, setLibrary] = useState<Media[]>([]);
  const [dashboard, setDashboard] = useState(blankDashboard);
  const [account, setAccount] = useState<Account | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [busy, setBusy] = useState(false);
  const [selected, setSelected] = useState<Media | null>(null);
  const [selectedCatalogue, setSelectedCatalogue] = useState<CatalogueTitle | null>(null);
  const [catalogueDetailLoading, setCatalogueDetailLoading] = useState(false);
  const [discoveryRows, setDiscoveryRows] = useState<Record<string, CatalogueTitle[]>>({});
  const [rowErrors, setRowErrors] = useState<Record<string, string>>({});
  const [rowLoading, setRowLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [librarySearch, setLibrarySearch] = useState("");
  const [libraryType, setLibraryType] = useState("ALL");
  const [suggestions, setSuggestions] = useState<CatalogueTitle[]>([]);
  const [suggestionOpen, setSuggestionOpen] = useState(false);
  const [suggestionBusy, setSuggestionBusy] = useState(false);
  const [suggestionError, setSuggestionError] = useState("");
  const [activeSuggestion, setActiveSuggestion] = useState(-1);
  const [featureIndex, setFeatureIndex] = useState(0);
  const [heroFailed, setHeroFailed] = useState(false);
  const catalogueRequestId = useRef(0);
  const suggestionRequestId = useRef(0);

  const loadAccount = useCallback(async () => {
    try {
      const current = await api.me(); setAccount(current);
      const [nextDashboard, titles] = await Promise.all([api.dashboard(), api.media()]);
      setDashboard(nextDashboard); setLibrary(titles);
    } catch { setAccount(null); }
  }, []);

  const loadCatalogue = useCallback(async () => {
    const requestId = ++catalogueRequestId.current;
    setLoading(true); setError("");
    try {
      const result = await api.catalogue(category, appliedQuery, page);
      if (requestId !== catalogueRequestId.current) return;
      setCatalogue(result.results); setTotalPages(result.totalPages);
    } catch (reason) {
      if (requestId !== catalogueRequestId.current) return;
      setCatalogue([]); setTotalPages(0);
      setError(reason instanceof Error ? reason.message : "Catalogue could not be loaded.");
    } finally { if (requestId === catalogueRequestId.current) setLoading(false); }
  }, [appliedQuery, category, page]);

  const loadDiscoveryRows = useCallback(async () => {
    setRowLoading(true);
    const responses = await Promise.all(discoverySections.map(async (section) => {
      try {
        const result = await api.catalogue(section.category, "", 1);
        setDiscoveryRows((current) => ({ ...current, [section.key]: result.results }));
        setRowErrors((current) => { const next = { ...current }; delete next[section.key]; return next; });
      } catch (reason) {
        setRowErrors((current) => ({ ...current, [section.key]: reason instanceof Error ? reason.message : "This catalogue is unavailable." }));
      }
    }));
    void responses;
    setRowLoading(false);
  }, []);

  useEffect(() => { void loadAccount(); void loadCatalogue(); void loadDiscoveryRows(); }, [loadAccount, loadCatalogue, loadDiscoveryRows]);

  useEffect(() => {
    const term = query.trim();
    if (term.length < 2 || view !== "discover") { setSuggestions([]); setSuggestionBusy(false); return; }
    const requestId = ++suggestionRequestId.current;
    const timer = window.setTimeout(async () => {
      setSuggestionBusy(true); setSuggestionError("");
      try {
        const result = await api.catalogue(category, term, 1);
        if (requestId === suggestionRequestId.current) setSuggestions(result.results.slice(0, 6));
      } catch (reason) {
        if (requestId === suggestionRequestId.current) { setSuggestions([]); setSuggestionError(reason instanceof Error ? reason.message : "Suggestions unavailable."); }
      } finally { if (requestId === suggestionRequestId.current) setSuggestionBusy(false); }
    }, 300);
    return () => window.clearTimeout(timer);
  }, [query, category, view]);

  const heroCandidates = Object.values(discoveryRows).flat().filter((title) => title.backdropUrl);
  const featured = heroCandidates.length ? heroCandidates[featureIndex % heroCandidates.length] : null;
  useEffect(() => {
    if (heroCandidates.length < 2) return;
    const timer = window.setInterval(() => setFeatureIndex((current) => (current + 1) % heroCandidates.length), 9000);
    return () => window.clearInterval(timer);
  }, [heroCandidates.length]);
  useEffect(() => setHeroFailed(false), [featured?.backdropUrl]);

  async function refreshLibrary(user: Account | null = account) {
    if (!user) return;
    const [nextDashboard, titles] = await Promise.all([api.dashboard(), api.media()]);
    setDashboard(nextDashboard); setLibrary(titles);
  }

  async function authenticate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    const form = new FormData(event.currentTarget);
    try {
      const email = String(form.get("email")); const password = String(form.get("password"));
      const user = authMode === "login" ? await api.login(email, password) : await api.register(email, String(form.get("displayName")), password);
      if (authMode === "register") {
        setAuthMode("login"); setError("Account created. Sign in with your new credentials.");
      } else { setAccount(user); setAuthOpen(false); await refreshLibrary(user); }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Authentication failed."); }
    finally { setBusy(false); }
  }

  async function signOut() {
    try { await api.logout(); setAccount(null); setLibrary([]); setDashboard(blankDashboard); setSelected(null); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not sign out."); }
  }

  async function importTitle(title: CatalogueTitle) {
    if (!account) { setAuthMode("login"); setAuthOpen(true); return; }
    setBusy(true); setError("");
    try { const saved = await api.importTitle(title); await refreshLibrary(); setSelected(saved); setView("library"); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not add this title to your library."); }
    finally { setBusy(false); }
  }

  async function openCatalogueTitle(title: CatalogueTitle) {
    setSelectedCatalogue(title);
    setCatalogueDetailLoading(true);
    try { setSelectedCatalogue(await api.catalogueDetails(title)); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Title details could not be loaded."); }
    finally { setCatalogueDetailLoading(false); }
  }

  async function updateTitle(update: Partial<Pick<Media, "watchStatus" | "personalRating" | "review" | "favourite" | "watchlist">> & { clearRating?: boolean }) {
    if (!selected) return;
    try { const saved = await api.updatePersonal(selected.id, update); setSelected(saved); await refreshLibrary(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not save title changes."); }
  }

  async function toggleEpisode(id: number, watched: boolean) {
    try { const saved = await api.updateEpisode(id, !watched); setSelected(saved); await refreshLibrary(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not update episode progress."); }
  }

  async function deleteTitle(media: Media) {
    if (!window.confirm(`Delete “${media.title}” and its episode progress from your library? This cannot be undone.`)) return;
    try { await api.deleteMedia(media.id); setSelected(null); setNotice(`${media.title} was removed from your library.`); setError(""); await refreshLibrary(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not delete this title."); }
  }

  async function syncEpisodes() {
    if (!selected) return;
    try { const saved = await api.syncEpisodes(selected.id); setSelected(saved); await refreshLibrary(); setNotice("Episode list loaded from the catalogue."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Episode metadata could not be loaded."); }
  }

  async function addEpisode(episode: { seasonNumber: number; episodeNumber: number; title: string }) {
    if (!selected) return;
    try { const saved = await api.addEpisode(selected.id, episode); setSelected(saved); await refreshLibrary(); setNotice("Episode added."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Episode could not be added."); }
  }

  const visibleLibrary = library.filter((item) =>
    (statusFilter === "ALL" || item.watchStatus === statusFilter)
    && (libraryType === "ALL" || (libraryType === "MOVIE" ? item.mediaType === "MOVIE" : item.mediaType === "SERIES"))
    && item.title.toLocaleLowerCase().includes(librarySearch.trim().toLocaleLowerCase()));
  const activeCategory = categories.find((item) => item.value === category)!;

  return <main className="app-shell">
    <aside className="sidebar">
      <button className="brand" onClick={() => setView("discover")}><FilmReel size={25} weight="fill" /><span>REELVAULT</span></button>
      <p className="side-label">YOUR VAULT</p>
      <button className={`side-link ${view === "discover" ? "selected" : ""}`} onClick={() => setView("discover")}><MagnifyingGlass size={19} /> Discover</button>
      <button className={`side-link ${view === "library" ? "selected" : ""}`} onClick={() => setView("library")}><BookmarkSimple size={19} /> My Library <span>{account ? library.length : "—"}</span></button>
      <div className="sidebar-bottom">
        {account ? <><div className="account-chip"><UserCircle size={22} /><span><strong>{account.displayName}</strong><small>{account.email}</small></span></div><button className="side-link" onClick={() => void signOut()}><SignOut size={19} /> Sign out</button></> : <button className="button primary full-width" onClick={() => { setAuthMode("login"); setAuthOpen(true); }}>Sign in</button>}
        <small className="provider-caption">Your library is private to your account.</small>
      </div>
    </aside>
    <section className="main-column">
      <header className="topbar">
        <div className="breadcrumbs"><span>REELVAULT</span><span>/</span><strong>{view === "discover" ? "Discover" : "My Library"}</strong></div>
        <div className="top-actions"><div className="suggestion-wrap"><label className="top-search"><MagnifyingGlass size={17} /><input role="combobox" aria-expanded={suggestionOpen && query.trim().length >= 2} aria-controls="title-suggestions" aria-autocomplete="list" value={query} onChange={(e) => { setQuery(e.target.value); setActiveSuggestion(-1); setSuggestionOpen(true); }} onFocus={() => setSuggestionOpen(true)} onBlur={() => window.setTimeout(() => setSuggestionOpen(false), 120)} onKeyDown={(e) => { if (e.key === "ArrowDown" && suggestions.length) { e.preventDefault(); setActiveSuggestion((i) => (i + 1) % suggestions.length); } else if (e.key === "ArrowUp" && suggestions.length) { e.preventDefault(); setActiveSuggestion((i) => (i <= 0 ? suggestions.length - 1 : i - 1)); } else if (e.key === "Escape") setSuggestionOpen(false); else if (e.key === "Enter") { e.preventDefault(); const match = suggestions[activeSuggestion]; if (match) { setSuggestionOpen(false); void openCatalogueTitle(match); } else { setPage(1); setAppliedQuery(query.trim()); setSuggestionOpen(false); } } }} placeholder="Search titles" /></label>{suggestionOpen && query.trim().length >= 2 && <div className="suggestion-menu" id="title-suggestions" role="listbox" aria-label="Title suggestions">{suggestionBusy ? <p>Searching live catalogue…</p> : suggestionError ? <p>{suggestionError}</p> : suggestions.length ? suggestions.map((title, index) => <button key={`${title.provider}-${title.externalId}`} role="option" aria-selected={activeSuggestion === index} onMouseDown={(event) => event.preventDefault()} onClick={() => { setSuggestionOpen(false); void openCatalogueTitle(title); }}><Poster url={title.posterUrl} title={title.title} /><span><strong>{title.title}</strong><small>{title.year ?? "Year unavailable"} · {categories.find((item) => item.value === title.category)?.label}</small></span></button>) : <p>No matching titles found.</p>}</div>}</div>{account ? <button className="account-button" onClick={() => void signOut()}><UserCircle size={19} /> {account.displayName}<SignOut size={16} /></button> : <button className="button primary compact" onClick={() => setAuthOpen(true)}>Sign in</button>}</div>
      </header>
      {error && <div className="alert page-alert" role="alert"><span>{error}</span><button onClick={() => setError("")} aria-label="Dismiss"><X size={18} /></button></div>}
      {notice && <div className="notice page-alert" role="status"><span>{notice}</span><button onClick={() => setNotice("")} aria-label="Dismiss"><X size={18} /></button></div>}

      {view === "discover" ? <>
        <section className="discover-hero">
          <img src={!heroFailed && featured?.backdropUrl ? featured.backdropUrl : "/art/catalogue-hero.png"} onError={() => setHeroFailed(true)} alt={!heroFailed && featured?.backdropUrl ? `${featured.title} backdrop` : "Abstract cinematic archive artwork"} />
          <div className="discover-shade" />
          <div className="discover-copy"><p className="eyebrow">{featured ? "FEATURED FROM THE LIVE CATALOGUE" : "A PERSONAL ARCHIVE FOR WHAT YOU WATCH"}</p><h1>{featured?.title ?? "Find your next great story."}</h1><p>{featured?.description ? formatSynopsis(featured.description) : "Explore films, television and anime. Save the titles that matter to your private vault."}</p><button className="button primary" onClick={() => featured ? void openCatalogueTitle(featured) : document.getElementById("catalogue")?.scrollIntoView({ behavior: "smooth" })}>{featured ? "View details" : "Explore catalogue"} <ArrowRight size={17} /></button></div>
          {heroCandidates.length > 1 && <div className="hero-picker" aria-label="Featured titles">{heroCandidates.slice(0, 8).map((title, index) => <button key={`${title.provider}-${title.externalId}`} aria-label={`Feature ${title.title}`} aria-pressed={index === featureIndex % heroCandidates.length} className={index === featureIndex % heroCandidates.length ? "active" : ""} onClick={() => setFeatureIndex(index)} />)}</div>}
          {account && <div className="vault-total"><span>IN YOUR VAULT</span><strong>{dashboard.stats.totalTitles}</strong><small>titles collected</small></div>}
        </section>
        <section className="discovery-rails" aria-label="Popular titles by category">
          {discoverySections.map((section) => <div className="discovery-rail" key={section.key}>
            <div className="rail-heading"><div><p className="eyebrow">{categories.find((item) => item.value === section.category)?.provider} CATALOGUE</p><h2>{section.title}</h2></div><button className="text-link" onClick={() => { setCategory(section.category); setPage(1); setAppliedQuery(""); setQuery(""); document.getElementById("catalogue")?.scrollIntoView({ behavior: "smooth" }); }}>Browse all <ArrowRight size={15} /></button></div>
            {rowLoading && !discoveryRows[section.key] ? <div className="rail-cards" aria-label={`Loading ${section.title}`}>{Array.from({ length: 6 }, (_, i) => <div className="rail-skeleton" key={i} />)}</div> : discoveryRows[section.key]?.length ? <div className="rail-cards">{discoveryRows[section.key].map((title) => <CatalogueCard key={`${title.provider}-${title.externalId}`} title={title} onOpen={() => void openCatalogueTitle(title)} onAdd={() => void importTitle(title)} adding={busy} />)}</div> : <div className="rail-empty"><span>{rowErrors[section.key] || "No titles are available in this catalogue right now."}</span><button className="text-link" onClick={() => void loadDiscoveryRows()} disabled={rowLoading}>Retry <ArrowRight size={14} /></button></div>}
          </div>)}
        </section>
        <section id="catalogue" className="catalogue-section">
          <div className="section-heading"><div><p className="eyebrow">CURATED FROM LIVE CATALOGUES</p><h2>Browse the catalogue</h2></div><span className="catalogue-count">{loading ? "Loading…" : `${catalogue.length} titles on this page`}</span></div>
          <div className="category-tabs" role="tablist" aria-label="Catalogue category">{categories.map((item) => <button key={item.value} role="tab" aria-selected={category === item.value} className={category === item.value ? "active" : ""} onClick={() => { setCategory(item.value); setPage(1); }}>{item.label}</button>)}</div>
          <form className="catalogue-filter" onSubmit={(e) => { e.preventDefault(); setPage(1); setAppliedQuery(query.trim()); if (query.trim() === appliedQuery) void loadCatalogue(); }}><label><MagnifyingGlass size={18} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={`Search ${activeCategory.label.toLowerCase()}…`} /><button className="button primary compact" type="submit">Search</button></label><span>{activeCategory.provider === "TMDB" ? "Powered by TMDB" : "Powered by AniList"}</span></form>
          {loading ? <div className="catalogue-grid" aria-label="Loading titles">{Array.from({ length: 10 }, (_, i) => <div key={i} className="catalogue-skeleton" />)}</div> : catalogue.length ? <div className="catalogue-grid">{catalogue.map((title) => <CatalogueCard key={`${title.provider}-${title.externalId}`} title={title} onOpen={() => void openCatalogueTitle(title)} onAdd={() => void importTitle(title)} adding={busy} />)}</div> : <div className="empty-state"><FilmReel size={34} /><h3>{error ? "Catalogue unavailable" : "No titles found"}</h3><p>{error || "Try another search or switch to a different catalogue."}</p><button className="button secondary" onClick={() => void loadCatalogue()}>Try again</button></div>}
          <div className="pagination"><button className="button secondary" disabled={page <= 1 || loading} onClick={() => setPage((n) => Math.max(1, n - 1))}>Previous</button><span>Page {page}{totalPages ? ` of ${totalPages}` : ""}</span><button className="button secondary" disabled={!totalPages || page >= totalPages || loading} onClick={() => setPage((n) => n + 1)}>Next</button></div>
          <div className="attribution"><a className="tmdb-credit" href="https://www.themoviedb.org/" target="_blank" rel="noreferrer" aria-label="The Movie Database (TMDB)"><img src="https://www.themoviedb.org/assets/2/v4/logos/v2/blue_square_2-d537fb228cf3ded904ef09b136fe3fec72548ebc1fea3fbbd1ad9e36364db38b.svg" alt="TMDB" /></a><span>Movie and TV data provided by TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB. Anime data provided by <a href="https://anilist.co/" target="_blank" rel="noreferrer">AniList</a>.</span></div>
        </section>
      </> : <>
        <section className="library-heading"><p className="eyebrow">YOUR PERSONAL COLLECTION</p><h1>My Library</h1><p>Every title, note and episode—kept private to your account.</p><div className="library-stats"><span><strong>{dashboard.stats.totalTitles}</strong> titles</span><span><strong>{dashboard.stats.movieCount}</strong> films</span><span><strong>{dashboard.stats.seriesCount}</strong> series</span><span><strong>{dashboard.stats.completedCount}</strong> completed</span></div></section>
        {!account ? <div className="empty-state"><BookmarkSimple size={36} /><h3>Your library is waiting</h3><p>Sign in to view and manage your personal collection.</p><button className="button primary" onClick={() => setAuthOpen(true)}>Sign in</button></div> : <>
          {dashboard.continueWatching.length > 0 && <section className="resume-section"><div className="library-toolbar"><h2>Continue watching</h2><span>{dashboard.continueWatching.length} active</span></div><div className="watching-strip">{dashboard.continueWatching.map((media) => <button className="resume-card" key={media.id} onClick={() => setSelected(media)}><Poster url={media.posterUrl} title={media.title} /><span><strong>{media.title}</strong><small>{media.mediaType === "SERIES" && media.episodes.length ? `${media.episodes.filter((episode) => episode.watched).length}/${media.episodes.length} episodes · ` : ""}{media.progressPercent}% complete</small><i><b style={{ width: `${media.progressPercent}%` }} /></i></span></button>)}</div></section>}
          <section className="library-section"><div className="library-toolbar"><h2>All titles <small>{visibleLibrary.length}</small></h2><div className="library-filters"><label className="library-search"><MagnifyingGlass size={16} /><input aria-label="Search your library" value={librarySearch} onChange={(e) => setLibrarySearch(e.target.value)} placeholder="Search your library" /></label><select aria-label="Filter by media type" value={libraryType} onChange={(e) => setLibraryType(e.target.value)}><option value="ALL">All types</option><option value="MOVIE">Movies</option><option value="SERIES">Series</option></select><select aria-label="Filter by watch status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option value="ALL">All statuses</option>{statuses.map((status) => <option key={status} value={status}>{status.replaceAll("_", " ")}</option>)}</select></div></div>{visibleLibrary.length ? <div className="poster-grid">{visibleLibrary.map((media) => <button className="library-card" key={media.id} onClick={() => setSelected(media)}><Poster url={media.posterUrl} title={media.title} /><div><strong>{media.title}</strong><small>{media.releaseYear} · {media.watchStatus.replaceAll("_", " ")}</small>{media.mediaType === "SERIES" && media.episodes.length > 0 && <small>{media.episodes.filter((episode) => episode.watched).length}/{media.episodes.length} episodes · {media.progressPercent}%</small>}</div></button>)}</div> : <div className="empty-state"><FilmReel size={36} /><h3>{library.length ? "No titles match these filters" : "Start your collection"}</h3><p>{library.length ? "Try another title, type or watch status." : "Browse the live catalogue and add a title to make it yours."}</p><button className="button primary" onClick={() => library.length ? (setLibrarySearch(""), setLibraryType("ALL"), setStatusFilter("ALL")) : setView("discover")}>{library.length ? "Clear filters" : "Browse catalogue"}</button></div>}</section>
        </>}
      </>}
      <footer className="site-footer"><span>REELVAULT · YOUR STORIES, IN ONE PLACE</span><a href="https://www.themoviedb.org/" target="_blank" rel="noreferrer">TMDB</a><a href="https://anilist.co/" target="_blank" rel="noreferrer">AniList</a><a href="#catalogue">Credits</a></footer>
    </section>

    {authOpen && <AuthDialog mode={authMode} setMode={setAuthMode} busy={busy} onClose={() => setAuthOpen(false)} onSubmit={authenticate} />}
    {selected && <MediaDialog media={selected} onClose={() => setSelected(null)} onSave={updateTitle} onEpisode={toggleEpisode} onDelete={() => void deleteTitle(selected)} onSyncEpisodes={() => void syncEpisodes()} onAddEpisode={addEpisode} />}
    {selectedCatalogue && <CatalogueDialog title={selectedCatalogue} loading={catalogueDetailLoading} busy={busy} onClose={() => setSelectedCatalogue(null)} onAdd={() => void importTitle(selectedCatalogue)} />}
  </main>;
}

function Poster({ url, title }: { url: string | null; title: string }) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [url]);
  return url && !failed ? <img className="catalogue-poster" src={url} alt={`${title} poster`} loading="lazy" onError={() => setFailed(true)} /> : <span className="poster-unavailable">POSTER<br />UNAVAILABLE</span>;
}

function formatSynopsis(value: string) {
  return value.replace(/&lt;\s*br\s*\/?\s*&gt;/gi, "\n").replace(/<\s*br\s*\/?>/gi, "\n")
    .replace(/&lt;\/?(?:p|div|li)[^&]*?&gt;/gi, "\n").replace(/<\/(p|div|li)\s*>/gi, "\n")
    .replace(/<[^>]*>/g, "").replace(/&nbsp;/gi, " ").replace(/&amp;/gi, "&")
    .replace(/&quot;/gi, '"').replace(/&#39;|&apos;/gi, "'").replace(/&lt;/gi, "<").replace(/&gt;/gi, ">")
    .replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
}

function DetailArtwork({ backdrop, poster }: { backdrop: string | null; poster: string | null }) {
  const [failed, setFailed] = useState(false);
  const source = backdrop || poster;
  useEffect(() => setFailed(false), [source]);
  return source && !failed ? <img src={source} alt="" onError={() => setFailed(true)} /> : <span>REELVAULT</span>;
}

function CatalogueCard({ title, onOpen, onAdd, adding }: { title: CatalogueTitle; onOpen: () => void; onAdd: () => void; adding: boolean }) {
  const detail = title.episodeCount ? `${title.episodeCount} eps` : title.runtimeMinutes ? `${title.runtimeMinutes} min` : title.season ? title.season : null;
  return <article className="catalogue-card"><button className="catalogue-card-art" onClick={onOpen} aria-label={`View details for ${title.title}`}><Poster url={title.posterUrl} title={title.title} /><span className="catalogue-kind">{categories.find((c) => c.value === title.category)?.label}</span>{title.rating !== null && <span className="catalogue-rating">★ {title.rating.toFixed(1)}</span>}</button><div className="catalogue-card-copy"><button className="catalogue-title" onClick={onOpen}>{title.title}</button><div className="catalogue-meta"><span>{title.year ?? "Year unknown"}</span>{title.format && <span>{title.format.replaceAll("_", " ")}</span>}{detail && <span>{detail}</span>}</div><p>{title.genres.slice(0, 3).join(" · ") || "Genres unavailable"}</p><button className="button secondary full-width" onClick={onAdd} disabled={adding}><Plus size={16} /> Add to my vault</button></div></article>;
}

function CatalogueDialog({ title, loading, busy, onClose, onAdd }: { title: CatalogueTitle; loading: boolean; busy: boolean; onClose: () => void; onAdd: () => void }) {
  return <div className="dialog-backdrop" onMouseDown={onClose}><aside className="detail-panel" role="dialog" aria-modal="true" aria-label={`${title.title} details`} onMouseDown={(e) => e.stopPropagation()}><button className="icon-button close" onClick={onClose} aria-label="Close"><X size={22} /></button><div className="detail-visual"><DetailArtwork backdrop={title.backdropUrl} poster={title.posterUrl} /></div><div className="detail-copy"><p className="eyebrow">{categories.find((item) => item.value === title.category)?.label} · {title.year ?? "Year unavailable"}</p><h2>{title.title}</h2><p className="genres">{title.genres.join(" · ") || "Genre information unavailable"}</p>{title.rating !== null && <p>Catalogue rating · {title.rating.toFixed(1)} / 10</p>}<p className="synopsis">{loading ? "Loading full title details…" : formatSynopsis(title.description || "No synopsis is available from this provider.")}</p><button className="button primary" onClick={onAdd} disabled={busy}><Plus size={17} /> Add to my vault</button></div></aside></div>;
}

function AuthDialog({ mode, setMode, busy, onClose, onSubmit }: { mode: "login" | "register"; setMode: (mode: "login" | "register") => void; busy: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="dialog-backdrop" onMouseDown={onClose}><section className="add-dialog auth-dialog" role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}><div className="dialog-heading"><div><p className="eyebrow">PRIVATE BY DESIGN</p><h2>{mode === "login" ? "Welcome back" : "Create your vault"}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><X size={21} /></button></div><form className="add-form" onSubmit={onSubmit}>{mode === "register" && <label>Display name<input name="displayName" required maxLength={80} autoComplete="name" /></label>}<label>Email<input name="email" required type="email" autoComplete="email" /></label><label>Password<input name="password" required type="password" minLength={10} autoComplete={mode === "login" ? "current-password" : "new-password"} /></label><button className="button primary full-width" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"}</button></form><p className="auth-switch">{mode === "login" ? "New to Reelvault?" : "Already have an account?"} <button onClick={() => setMode(mode === "login" ? "register" : "login")}>{mode === "login" ? "Create an account" : "Sign in"}</button></p></section></div>;
}

function MediaDialog({ media, onClose, onSave, onEpisode, onDelete, onSyncEpisodes, onAddEpisode }: { media: Media; onClose: () => void; onSave: (update: Partial<Pick<Media, "watchStatus" | "personalRating" | "review" | "favourite" | "watchlist">> & { clearRating?: boolean }) => void; onEpisode: (id: number, watched: boolean) => void; onDelete: () => void; onSyncEpisodes: () => void; onAddEpisode: (episode: { seasonNumber: number; episodeNumber: number; title: string }) => void }) {
  const [rating, setRating] = useState(media.personalRating?.toString() ?? "");
  const [review, setReview] = useState(media.review ?? "");
  const [season, setSeason] = useState("1");
  const [episodeNumber, setEpisodeNumber] = useState("");
  const [episodeTitle, setEpisodeTitle] = useState("");
  useEffect(() => { setRating(media.personalRating?.toString() ?? ""); setReview(media.review ?? ""); }, [media]);
  const watchedCount = media.episodes.filter((episode) => episode.watched).length;
  return <div className="dialog-backdrop" onMouseDown={onClose}><aside className="detail-panel" role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}><button className="icon-button close" onClick={onClose} aria-label="Close"><X size={22} /></button><div className="detail-visual"><DetailArtwork backdrop={media.backdropUrl} poster={media.posterUrl} /></div><div className="detail-copy"><p className="eyebrow">{media.catalogueCategory?.replaceAll("_", " ") ?? media.mediaType} · {media.releaseYear}</p><h2>{media.title}</h2><p className="genres">{media.genres.join(" · ")}</p><p className="synopsis">{formatSynopsis(media.description || "No synopsis available.")}</p><div className="quick-actions"><button className={`icon-action ${media.favourite ? "selected" : ""}`} onClick={() => onSave({ favourite: !media.favourite })}><Heart size={18} /> Favourite</button><button className={`icon-action ${media.watchlist ? "selected" : ""}`} onClick={() => onSave({ watchlist: !media.watchlist })}><BookmarkSimple size={18} /> Watchlist</button></div><label className="detail-status">Watch status<select value={media.watchStatus} onChange={(e) => onSave({ watchStatus: e.target.value as WatchStatus })}>{statuses.map((s) => <option key={s} value={s}>{s.replaceAll("_", " ")}</option>)}</select></label><form className="personal-form personal-notes" onSubmit={(e) => { e.preventDefault(); onSave({ personalRating: rating ? Number(rating) : null, clearRating: !rating, review }); }}><label>Personal rating<select value={rating} onChange={(e) => setRating(e.target.value)}><option value="">Not rated</option>{Array.from({ length: 10 }, (_, i) => <option key={i + 1} value={i + 1}>{i + 1} / 10</option>)}</select></label><label className="full-width">Private review<textarea rows={3} maxLength={4000} value={review} onChange={(e) => setReview(e.target.value)} placeholder="Keep a note for yourself" /></label><button className="button primary full-width">Save rating and review</button></form><div className="episodes">{media.mediaType === "SERIES" && <><div className="episodes-heading"><TelevisionSimple size={20} /><h3>Episode progress</h3><span>{watchedCount}/{media.episodes.length} watched · {media.progressPercent}%</span>{media.provider && media.externalProviderId && <button className="button secondary" onClick={onSyncEpisodes}>Sync missing episodes</button>}</div>{media.episodes.length ? media.episodes.map((episode) => <button className={`episode ${episode.watched ? "watched" : ""}`} key={episode.id} aria-pressed={episode.watched} onClick={() => onEpisode(episode.id, episode.watched)}><span className="episode-mark">{episode.watched && <Check size={14} />}</span><span>S{episode.seasonNumber} E{episode.episodeNumber}</span><strong>{episode.title}</strong><small>{episode.watched ? "Watched · click to undo" : "Not watched · click to mark"}</small></button>) : <><p className="muted-copy">No episode records are attached yet.</p>{!media.provider && !media.externalProviderId && <p className="muted-copy">Add an episode below to begin tracking.</p>}</>}<form className="episode-add" onSubmit={(event) => { event.preventDefault(); const n = Number(episodeNumber); if (!Number.isInteger(n) || n < 1) return; onAddEpisode({ seasonNumber: Math.max(1, Number(season) || 1), episodeNumber: n, title: episodeTitle.trim() || `Episode ${n}` }); setEpisodeNumber(""); setEpisodeTitle(""); }}><label>Season<input type="number" min="1" value={season} onChange={(event) => setSeason(event.target.value)} /></label><label>Episode<input type="number" min="1" required value={episodeNumber} onChange={(event) => setEpisodeNumber(event.target.value)} /></label><label>Title<input maxLength={200} value={episodeTitle} onChange={(event) => setEpisodeTitle(event.target.value)} placeholder="Optional" /></label><button className="button secondary" type="submit">Add episode</button></form></>}</div><button className="button danger full-width" onClick={onDelete}><Trash size={17} /> Delete from my library</button></div></aside></div>;
}
