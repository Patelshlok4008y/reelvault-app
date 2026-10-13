export type MediaType = "MOVIE" | "SERIES";
export type CatalogueCategory = "MOVIE" | "TV_SERIES" | "ANIME_MOVIE" | "ANIME_SERIES";
export type WatchStatus = "PLAN_TO_WATCH" | "WATCHING" | "COMPLETED" | "ON_HOLD" | "DROPPED";
export type Account = { id: number; email: string; displayName: string; createdAt: string };
export type CatalogueTitle = {
  provider: "TMDB" | "ANILIST"; externalId: string; category: CatalogueCategory; mediaType: MediaType;
  title: string; year: number | null; description: string; genres: string[]; posterUrl: string | null;
  backdropUrl: string | null; rating: number | null; runtimeMinutes: number | null;
  episodeCount: number | null; seasonCount: number | null; format: string | null; season: string | null;
  seasons: { seasonNumber: number; episodeCount: number }[];
};
export type CataloguePage = { page: number; perPage: number; totalResults: number; totalPages: number; results: CatalogueTitle[] };
export type Episode = { id: number; seasonNumber: number; episodeNumber: number; title: string; runtimeMinutes: number | null; watched: boolean };
export type Media = {
  id: number; mediaType: MediaType; title: string; releaseYear: number; description: string; genres: string[];
  posterUrl: string | null; backdropUrl: string | null; provider?: string; externalProviderId?: string;
  externalRating?: number | null; catalogueCategory?: CatalogueCategory; runtimeMinutes: number | null;
  personalRating: number | null; review: string | null; favourite: boolean; watchlist: boolean;
  watchStatus: WatchStatus; progressPercent: number; createdAt: string; updatedAt: string; episodes: Episode[];
};
export type Dashboard = {
  featured: Media | null;
  stats: { totalTitles: number; movieCount: number; seriesCount: number; completedCount: number; favouriteCount: number };
  continueWatching: Media[]; recentlyAdded: Media[];
};
export type MediaInput = { mediaType: MediaType; title: string; releaseYear: number; description?: string; genres?: string; posterUrl?: string; backdropUrl?: string; runtimeMinutes?: number };

// Keep browser requests same-origin so the Next.js rewrite can proxy cookies and
// CSRF headers without relying on cross-origin cookie/CORS behavior.
const API_ROOT = "";
let csrf: Promise<void> | null = null;
function cookie(name: string) { return document.cookie.split(";").map((value) => value.trim()).find((value) => value.startsWith(`${name}=`))?.slice(name.length + 1); }

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const method = options?.method?.toUpperCase() ?? "GET";
  if (!["GET", "HEAD", "OPTIONS"].includes(method) && path !== "/api/auth/csrf") {
    csrf ??= fetch(`${API_ROOT}/api/auth/csrf`, { credentials: "include", cache: "no-store" }).then((response) => {
      if (!response.ok) throw new Error("Unable to establish a secure session.");
    }).catch((reason: unknown) => {
      if (reason instanceof TypeError) throw new Error("The Reelvault API is unreachable. Start the backend and retry.");
      throw reason;
    }).finally(() => { csrf = null; });
    await csrf;
  }
  const headers = new Headers(options?.headers);
  if (options?.body) headers.set("Content-Type", "application/json");
  const token = typeof document === "undefined" ? undefined : cookie("XSRF-TOKEN");
  if (token && !["GET", "HEAD", "OPTIONS"].includes(method)) headers.set("X-XSRF-TOKEN", decodeURIComponent(token));
  let response: Response;
  try {
    response = await fetch(`${API_ROOT}${path}`, { ...options, method, headers, credentials: "include", cache: "no-store" });
  } catch (reason) {
    if (reason instanceof TypeError) throw new Error("The Reelvault API is unreachable. Check that the backend is running, then retry.");
    throw reason;
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.message ?? (response.status === 401 ? "Please sign in to continue." : "The request could not be completed."));
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  me: () => request<Account>("/api/auth/me"),
  login: (email: string, password: string) => request<Account>("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  register: (email: string, displayName: string, password: string) => request<Account>("/api/auth/register", { method: "POST", body: JSON.stringify({ email, displayName, password }) }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  dashboard: () => request<Dashboard>("/api/dashboard"),
  media: (query = "") => request<Media[]>(`/api/media${query}`),
  deleteMedia: (id: number) => request<void>(`/api/media/${id}`, { method: "DELETE" }),
  createMedia: (input: MediaInput) => request<Media>("/api/media", { method: "POST", body: JSON.stringify(input) }),
  importTitle: (title: CatalogueTitle) => request<Media>("/api/media/import", { method: "POST", body: JSON.stringify({ provider: title.provider, externalId: title.externalId, category: title.category }) }),
  catalogue: (category: CatalogueCategory, search: string, page: number) => request<CataloguePage>(`/api/catalogue/${category}?page=${page}${search.trim() ? `&search=${encodeURIComponent(search.trim())}` : ""}`),
  catalogueDetails: (title: CatalogueTitle) => request<CatalogueTitle>(`/api/catalogue/${title.category}/${encodeURIComponent(title.externalId)}?provider=${title.provider}`),
  updatePersonal: (id: number, update: Partial<Pick<Media, "watchStatus" | "personalRating" | "review" | "favourite" | "watchlist">> & { clearRating?: boolean }) => request<Media>(`/api/media/${id}/personal`, { method: "PATCH", body: JSON.stringify(update) }),
  updateEpisode: (id: number, watched: boolean) => request<Media>(`/api/episodes/${id}/watched`, { method: "PATCH", body: JSON.stringify({ watched }) }),
  syncEpisodes: (seriesId: number) => request<Media>(`/api/series/${seriesId}/episodes/sync`, { method: "POST" }),
  addEpisode: (seriesId: number, episode: { seasonNumber: number; episodeNumber: number; title: string; runtimeMinutes?: number | null }) => request<Media>(`/api/series/${seriesId}/episodes`, { method: "POST", body: JSON.stringify(episode) }),
};
