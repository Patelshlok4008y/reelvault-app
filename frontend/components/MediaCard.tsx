"use client";

import { Heart, Play, Star } from "@phosphor-icons/react";
import { motion, useReducedMotion } from "motion/react";
import type { Media } from "@/lib/api";

type MediaCardProps = {
  media: Media;
  onSelect: (media: Media) => void;
};

function initials(title: string) {
  return title.split(/\s+/).slice(0, 2).map((word) => word[0]).join("").toUpperCase();
}

export function MediaCard({ media, onSelect }: MediaCardProps) {
  const reducedMotion = useReducedMotion();
  const isSeries = media.mediaType === "SERIES";

  return (
    <motion.article
      className="media-card"
      initial={reducedMotion ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={reducedMotion ? undefined : { y: -4 }}
      transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
    >
      <button className="poster-button" onClick={() => onSelect(media)} aria-label={`Open ${media.title}`}>
        {media.posterUrl ? (
          // Native img preserves external URLs entered by the user without requiring Next image host setup.
          <img src={media.posterUrl} alt={`${media.title} poster`} className="poster-image" />
        ) : (
          <span className={`poster-fallback poster-tone-${Math.abs(media.id) % 6}`} aria-hidden="true">
            <small>{isSeries ? "SERIES" : "FILM"}</small>
            <strong>{media.title}</strong>
            <em>{media.releaseYear}</em>
          </span>
        )}
        <span className="poster-scrim" />
        <span className="media-kind">{isSeries ? "Series" : "Film"}</span>
        <span className="card-open"><Play size={18} weight="fill" /></span>
      </button>
      <div className="card-details">
        <button className="title-button" onClick={() => onSelect(media)}>{media.title}</button>
        <div className="card-meta">
          <span>{media.releaseYear}</span>
          {media.personalRating !== null && <span className="rating"><Star size={14} weight="fill" /> {media.personalRating}</span>}
          {media.favourite && <Heart size={15} weight="fill" className="heart" aria-label="Favourite" />}
        </div>
        {(media.watchStatus === "WATCHING" || media.progressPercent > 0) && (
          <div className="progress-line" aria-label={`${media.progressPercent}% watched`}>
            <span style={{ width: `${media.progressPercent}%` }} />
          </div>
        )}
      </div>
    </motion.article>
  );
}
