package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.GithubResponse;
import com.n33miaz.stats.service.LastFmService.MusicDashboardData;
import com.n33miaz.stats.service.LastFmService.SimpleItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SvgService {

    private static final int REPO_CARD_WIDTH = 400;
    private static final int REPO_LINE_HEIGHT = 20;
    private static final int REPO_MAX_DESC_LINES = 3;
    private static final int REPO_MAX_CHARS_PER_LINE = 55;

    // --- TESTE ---
    public String generateTestSvg(String text) {
        return """
                <svg width="400" height="100" xmlns="http://www.w3.org/2000/svg">
                    <rect width="100%%" height="100%%" fill="#0D1117" rx="10" ry="10"/>
                    <text x="50%%" y="50%%" dominant-baseline="middle" text-anchor="middle"
                          fill="#762075" font-family="Segoe UI, Helvetica, Arial, sans-serif" font-weight="bold" font-size="24">
                        %s
                    </text>
                </svg>
                """
                .formatted(escapeHtml(text));
    }

    // --- CARD DE REPOSITÓRIO (PIN) ---
    public String generateRepoCard(GithubResponse.Repository repo, Map<String, String> colors, boolean hideBorder) {
        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String iconColor = colors.getOrDefault("icon_color", "586069");
        String textColor = colors.getOrDefault("text_color", "434d58");
        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        String description = repo.description() != null ? repo.description() : "No description provided";
        String langName = repo.primaryLanguage() != null ? repo.primaryLanguage().name() : "N/A";
        String langColor = repo.primaryLanguage() != null ? repo.primaryLanguage().color() : "#ccc";
        String stars = kFormatter(repo.stargazerCount());
        String forks = kFormatter(repo.forkCount());

        // altura dinâmica baseada no texto
        List<String> descLines = wrapText(description, REPO_MAX_CHARS_PER_LINE, REPO_MAX_DESC_LINES);
        int descriptionHeight = descLines.size() * REPO_LINE_HEIGHT;
        int totalHeight = (descLines.size() > 1 ? 120 : 110) + descriptionHeight;
        int statusY = totalHeight - 25;

        StringBuilder descSvg = new StringBuilder();
        for (String line : descLines) {
            descSvg.append(String.format("<tspan x=\"25\" dy=\"%s\">%s</tspan>", "1.2em", escapeHtml(line)));
        }

        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <style>
                        .header { font: 600 18px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .desc { font: 400 13px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .stat { font: 600 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .icon { fill: #%s; }
                    </style>

                    <rect x="0.5" y="0.5" rx="4.5" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                    <g transform="translate(25, 35)">
                        <svg class="icon" x="0" y="-13" viewBox="0 0 16 16" version="1.1" width="16" height="16">
                            <path fill-rule="evenodd" d="M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 110-1.5h1.75v-2h-8a1 1 0 00-.714 1.7.75.75 0 01-1.072 1.05A2.495 2.495 0 012 11.5v-9zm10.5-1V9h-8c-.356 0-.694.074-1 .208V2.5a1 1 0 011-1h8zM5 12.25v3.25a.25.25 0 00.4.2l1.45-1.087a.25.25 0 01.3 0L8.6 15.7a.25.25 0 00.4-.2v-3.25a.25.25 0 00-.25-.25h-3.5a.25.25 0 00-.25.25z"></path>
                        </svg>
                        <text x="25" y="0" class="header">%s</text>
                    </g>

                    <g transform="translate(0, 45)">
                        <text x="25" y="-5" class="desc">
                            %s
                        </text>
                    </g>

                    <g transform="translate(30, %d)">
                        <g transform="translate(0, 0)">
                            <circle cx="0" cy="-5" r="6" fill="%s" />
                            <text data-testid="lang-name" x="15" class="stat">%s</text>
                        </g>
                        <g transform="translate(%d, 0)">
                            <svg class="icon" y="-12" viewBox="0 0 16 16" version="1.1" width="16" height="16">
                                 <path fill-rule="evenodd" d="M8 .25a.75.75 0 01.673.418l1.882 3.815 4.21.612a.75.75 0 01.416 1.279l-3.046 2.97.719 4.192a.75.75 0 01-1.088.791L8 12.347l-3.766 1.98a.75.75 0 01-1.088-.79l.72-4.194L.818 6.374a.75.75 0 01.416-1.28l4.21-.611L7.327.668A.75.75 0 018 .25z"></path>
                            </svg>
                            <text x="25" class="stat">%s</text>
                        </g>
                        <g transform="translate(%d, 0)">
                            <svg class="icon" y="-12" viewBox="0 0 16 16" version="1.1" width="16" height="16">
                                <path fill-rule="evenodd" d="M5 3.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm0 2.122a2.25 2.25 0 10-1.5 0v.878A2.25 2.25 0 005.75 8.5h1.5v2.128a2.251 2.251 0 101.5 0V8.5h1.5a2.25 2.25 0 002.25-2.25v-.878a2.25 2.25 0 10-1.5 0v.878a.75.75 0 01-.75.75h-4.5A.75.75 0 015 6.25v-.878zm3.75 7.378a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm3-8.75a.75.75 0 100-1.5.75.75 0 000 1.5z"></path>
                            </svg>
                            <text x="25" class="stat">%s</text>
                        </g>
                    </g>
                </svg>
                """
                .formatted(
                        REPO_CARD_WIDTH, totalHeight, REPO_CARD_WIDTH, totalHeight,
                        titleColor, textColor, textColor, iconColor,
                        REPO_CARD_WIDTH - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                        escapeHtml(repo.name()),
                        descSvg.toString(),
                        statusY,
                        langColor, escapeHtml(langName),
                        estimateTextWidth(langName) + 25,
                        stars,
                        estimateTextWidth(langName) + estimateTextWidth(stars) + 60,
                        forks);
    }

    // --- DASHBOARD DE MÚSICA ---
    public String generateMusicDashboard(MusicDashboardData data, java.util.Map<String, String> colors,
            boolean hideBorder, String periodText) {
        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String textColor = colors.getOrDefault("text_color", "434d58");
        String iconColor = colors.getOrDefault("icon_color", "1DB954");
        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        int dividerX = 340;
        int col1X = dividerX + 30;
        int col2X = dividerX + 250;

        // Configuração Esquerda (Imagem e Texto)
        int leftCenter = dividerX / 2;
        int imgSize = 120; // Tamanho aumentado
        int imgX = 25; // Alinhado à esquerda
        int imgY = 70; // Centralizado verticalmente na área útil
        int textX = imgX + imgSize + 15; // Texto logo após a imagem

        String coverImage = data.currentTrack().imageBase64().isEmpty()
                ? renderDefaultDisk(imgX + (imgSize / 2), imgY + (imgSize / 2), imgSize / 2)
                : String.format(
                        "<image x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" href=\"%s\" clip-path=\"url(#clip-main)\" class=\"album-art\"/>",
                        imgX, imgY, imgSize, imgSize, data.currentTrack().imageBase64());

        String statusText = data.currentTrack().isPlaying() ? "NOW PLAYING" : "LAST PLAYED";

        // Equalizador
        String equalizer = data.currentTrack().isPlaying() ? renderEqualizer(iconColor, textX, 50) : "";

        // Plays
        String playsBadge = data.currentTrack().userPlayCount() > 0
                ? String.format(
                        """
                                  <g transform="translate(%d, 240)">
                                      <rect x="-50" y="0" width="100" height="20" rx="10" fill="#%s" fill-opacity="0.15"/>
                                      <text x="0" y="14" text-anchor="middle" font-size="10" font-weight="bold" fill="#%s">%d plays</text>
                                  </g>
                                """,
                        leftCenter, titleColor, titleColor, data.currentTrack().userPlayCount())
                : "";

        String artistsList = renderList(data.topArtists(), col1X, 70, true, textColor, iconColor, false);
        String albumsList = renderList(data.topAlbums(), col2X, 70, false, textColor, iconColor, true);

        // Período
        int rightCenter = dividerX + (800 - dividerX) / 2;
        String periodBadge = String.format("""
                    <g transform="translate(%d, 240)">
                        <rect x="-40" y="0" width="80" height="20" rx="10" fill="#%s" fill-opacity="0.15"/>
                        <text x="0" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#%s">%s</text>
                    </g>
                """, rightCenter, titleColor, titleColor, periodText);

        return """
                    <svg width="800" height="280" viewBox="0 0 800 280" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <style>
                            .header { font: 700 10px 'Segoe UI', Ubuntu, Sans-Serif; letter-spacing: 1.5px; fill: #%s; opacity: 0.8; }
                            .title { font: 700 19px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                            .subtitle { font: 400 14px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; opacity: 0.9; }
                            .stat-title { font: 600 13px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                            .stat-sub { font: 400 11px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; opacity: 0.7; }
                            .section-header { font: 700 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; text-transform: uppercase; letter-spacing: 1px; }

                            .fade-in { animation: fadeIn 0.8s ease-in-out forwards; opacity: 0; }
                            @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

                            .album-art { filter: drop-shadow(0px 8px 16px rgba(0,0,0,0.3)); }
                        </style>

                        <rect x="0.5" y="0.5" rx="10" height="99%%" width="799" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                        <defs>
                            <clipPath id="clip-main"><rect x="%d" y="%d" width="%d" height="%d" rx="6" /></clipPath>
                            <clipPath id="clip-circle"><circle cx="16" cy="16" r="16" /></clipPath>
                            <clipPath id="clip-square"><rect x="0" y="0" width="32" height="32" rx="4" /></clipPath>
                        </defs>

                        <!-- ESQUERDA -->
                        <g class="fade-in" style="animation-delay: 0.1s">
                            <!-- Status Header -->
                            <text x="%d" y="40" text-anchor="middle" class="section-header">%s</text>

                            <!-- Cover Image -->
                            %s

                            <!-- Track Info (Alinhado à esquerda da imagem) -->
                            <text x="%d" y="120" text-anchor="start" class="title">%s</text>
                            <text x="%d" y="145" text-anchor="start" class="subtitle">%s</text>

                            %s <!-- Equalizer -->
                            %s <!-- Plays Badge -->
                        </g>

                        <!-- DIVIDER -->
                        <line x1="%d" y1="40" x2="%d" y2="240" stroke="#%s" stroke-width="1" stroke-opacity="0.2" />

                        <!-- DIREITA -->
                        <g class="fade-in" style="animation-delay: 0.3s">
                            <text x="%d" y="40" text-anchor="middle" class="section-header">Top Artists</text>
                            %s
                        </g>

                        <g class="fade-in" style="animation-delay: 0.5s">
                            <text x="%d" y="40" text-anchor="middle" class="section-header">Top Albums</text>
                            %s
                        </g>

                        <!-- Período -->
                        %s

                    </svg>
                """
                .formatted(
                        titleColor, titleColor, textColor,
                        titleColor, textColor, textColor,
                        bgColor, borderColor, hideBorder ? "0" : "1",
                        imgX, imgY, imgSize, imgSize,
                        leftCenter, statusText,
                        coverImage,
                        textX, escapeHtml(truncate(data.currentTrack().name(), 22)),
                        textX, escapeHtml(truncate(data.currentTrack().artist(), 25)),
                        equalizer, playsBadge,
                        dividerX, dividerX, textColor,
                        col1X + 80, artistsList,
                        col2X + 80, albumsList,
                        periodBadge);
    }

    // --- HELPERS ---
    private String renderList(List<SimpleItem> items, int x, int yStart, boolean isCircle, String titleColor,
            String subColor, boolean showExtra) {
        StringBuilder sb = new StringBuilder();
        int y = yStart;

        for (SimpleItem item : items) {
            String clipId = isCircle ? "clip-circle" : "clip-square";

            String imgContent;
            if (item.imageBase64() == null || item.imageBase64().isEmpty()) {
                if (isCircle) {
                    char initial = (item.title() == null || item.title().isEmpty()) ? '?' : item.title().charAt(0);
                    String color = String.format("%06x", (item.title().hashCode() & 0xFFFFFF));
                    imgContent = String.format(
                            """
                                    <circle cx='16' cy='16' r='16' fill='#%s' fill-opacity='0.5'/>
                                    <text x='16' y='21' text-anchor='middle' fill='#fff' font-weight='bold' font-size='14' font-family='Arial'>%s</text>
                                    """,
                            color, initial);
                } else {
                    imgContent = String.format(
                            "<image width='32' height='32' href='%s' clip-path='url(#%s)' preserveAspectRatio='xMidYMid slice' />",
                            item.imageBase64(), clipId);
                }
            } else {
                imgContent = String.format("<image width='32' height='32' href='%s' clip-path='url(#%s)' />",
                        item.imageBase64(), clipId);
            }

            String subtitle = item.subtitle();
            if (showExtra && item.extraInfo() != null) {
                subtitle = item.extraInfo();
            }

            sb.append(String.format("""
                        <g transform="translate(%d, %d)">
                            %s
                            <text x="42" y="14" class="stat-title">%s</text>
                            <text x="42" y="28" class="stat-sub">%s</text>
                        </g>
                    """, x, y, imgContent,
                    escapeHtml(truncate(item.title(), 20)),
                    escapeHtml(truncate(subtitle, 25))));

            y += 50;
        }
        return sb.toString();
    }

    // --- Equalizador Animado ---
    private String renderEqualizer(String color, int x, int y) {
        return """
                    <g transform="translate(%d, %d)">
                        <rect width="3" height="10" fill="#%s"><animate attributeName="height" values="10;20;10" dur="0.8s" repeatCount="indefinite" /></rect>
                        <rect x="5" width="3" height="18" fill="#%s"><animate attributeName="height" values="18;8;18" dur="0.8s" repeatCount="indefinite" begin="0.1s" /></rect>
                        <rect x="10" width="3" height="12" fill="#%s"><animate attributeName="height" values="12;22;12" dur="0.8s" repeatCount="indefinite" begin="0.2s" /></rect>
                        <rect x="15" width="3" height="16" fill="#%s"><animate attributeName="height" values="16;6;16" dur="0.8s" repeatCount="indefinite" begin="0.3s" /></rect>
                    </g>
                """
                .formatted(x, y, color, color, color, color);
    }

    // --- Disco Padrão ---
    private String renderDefaultDisk(int cx, int cy, int r) {
        return String.format(
                "<circle cx='%d' cy='%d' r='%d' fill='#222' /><circle cx='%d' cy='%d' r='%d' fill='#111' />", cx, cy, r,
                cx, cy, r / 3);
    }

    // --- UTILS ---

    private String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String truncate(String text, int max) {
        if (text == null)
            return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String kFormatter(int num) {
        if (num > 999) {
            return String.format("%.1fk", num / 1000.0);
        }
        return String.valueOf(num);
    }

    private List<String> wrapText(String text, int lineLimit, int maxLines) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > lineLimit) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                if (lines.size() >= maxLines) {
                    String lastLine = lines.get(lines.size() - 1);
                    if (lastLine.length() > 3)
                        lines.set(lines.size() - 1, lastLine.substring(0, lastLine.length() - 3) + "...");
                    return lines;
                }
            }
            if (!currentLine.isEmpty())
                currentLine.append(" ");
            currentLine.append(word);
        }
        if (!currentLine.isEmpty())
            lines.add(currentLine.toString());
        return lines;
    }

    private int estimateTextWidth(String text) {
        return (int) (text.length() * 7.5);
    }
}