import React from 'react';

interface RadafiqLogoProps {
  size?: number;
  className?: string;
}

/**
 * Radafiq logo — shield with upward trending arrow, coin stack, and Arabic letterforms.
 * Uses the high-resolution PNG from /logo.png
 */
export default function RadafiqLogo({ size = 48, className }: RadafiqLogoProps) {
  return (
    <img
      src="/logo.png"
      alt="Radafiq logo"
      width={size}
      height={size}
      className={className}
      style={{
        display: 'block',
        objectFit: 'contain',
        borderRadius: '22%',
      }}
    />
  );
}
