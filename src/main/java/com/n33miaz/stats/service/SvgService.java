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
    private static final int REPO_MAX_DESC_LINES = 2;
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

    // --- CARD DE REPOSITÓRIO (PIN) - VERSÃO CORRIGIDA FINAL ---
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

        int commitCount = (repo.object() != null && repo.object().history() != null)
                ? repo.object().history().totalCount()
                : 0;
        String commits = kFormatter(commitCount);

        List<String> descLines = wrapText(description, REPO_MAX_CHARS_PER_LINE, REPO_MAX_DESC_LINES);

        int paddingTop = 40;
        int headerHeight = 25;
        int descLineHeight = 22;
        int descBlockHeight = Math.max(descLines.size() * descLineHeight, 20); 
        int footerHeight = 40;
        int paddingBottom = 15;

        int totalHeight = paddingTop + headerHeight + descBlockHeight + footerHeight + paddingBottom;

        int headerY = 35; 
        int descStartY = headerY + 30;
        int footerY = totalHeight - 20; 

        int titleWidth = estimateTextWidth(repo.name());
        int iconSize = 20;
        int gapIconText = 10;
        int totalHeaderWidth = iconSize + gapIconText + titleWidth;
        int headerIconX = (REPO_CARD_WIDTH - totalHeaderWidth) / 2;
        int headerTextX = headerIconX + iconSize + gapIconText;

        StringBuilder descSvg = new StringBuilder();
        for (int i = 0; i < descLines.size(); i++) {
            int lineY = descStartY + (i * descLineHeight);
            descSvg.append(String.format(
                    "<text x=\"200\" y=\"%d\" text-anchor=\"middle\" class=\"desc\">%s</text>",
                    lineY,
                    escapeHtml(descLines.get(i))));
        }

        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <style>
                        .header { font: 700 18px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .desc { font: 400 13px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .stat { font: 600 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .icon { fill: #%s; }

                        .fade-in { opacity: 0; animation: fadeIn 0.6s ease-out forwards; }
                        .d-1 { animation-delay: 0.1s; }
                        .d-2 { animation-delay: 0.2s; }
                        .d-3 { animation-delay: 0.3s; }

                        @keyframes fadeIn {
                            from { opacity: 0; transform: translateY(10px); }
                            to { opacity: 1; transform: translateY(0); }
                        }
                    </style>

                    <rect x="0.5" y="0.5" rx="10" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                    <!-- HEADER -->
                    <g class="fade-in d-1">
                        <svg class="icon" x="%d" y="%d" viewBox="0 0 16 16" width="20" height="20" margin="1">
                            <path fill-rule="evenodd" d="M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 110-1.5h1.75v-2h-8a1 1 0 00-.714 1.7.75.75 0 01-1.072 1.05A2.495 2.495 0 012 11.5v-9zm10.5-1V9h-8c-.356 0-.694.074-1 .208V2.5a1 1 0 011-1h8zM5 12.25v3.25a.25.25 0 00.4.2l1.45-1.087a.25.25 0 01.3 0L8.6 15.7a.25.25 0 00.4-.2v-3.25a.25.25 0 00-.25-.25h-3.5a.25.25 0 00-.25.25z"/>
                        </svg>
                        <text x="%d" y="%d" class="header">%s</text>
                    </g>

                    <!-- DESCRIÇÃO -->
                    <g class="fade-in d-2">
                        %s
                    </g>

                    <!-- FOOTER -->
                    <g class="fade-in d-3">

                        <!-- ESQUERDA -->
                        <!-- Linguagem -->
                        <circle cx="30" cy="%d" r="5" fill="%s" />
                        <text x="40" y="%d" class="stat">%s</text>

                        <!-- Commits -->
                        <g transform="translate(%d, %d)">
                            <svg class="icon" y="-11" viewBox="0 0 16 16" width="14" height="14">
                                <path fill-rule="evenodd" d="M10.5 7.75a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0zm1.43.75a4.002 4.002 0 01-7.86 0H.75a.75.75 0 110-1.5h3.32a4.001 4.001 0 017.86 0h3.32a.75.75 0 110 1.5h-3.32z"/>
                            </svg>
                            <text x="18" class="stat">%s</text>
                        </g>

                        <!-- DIREITA -->
                        <g transform="translate(365, %d)">
                            <!-- Forks -->
                            <text x="10" text-anchor="end" class="stat">%s</text>
                            <svg class="icon" x="-15" y="-11" viewBox="0 0 16 16" width="14" height="14">
                                <path fill-rule="evenodd" d="M5 3.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm0 2.122a2.25 2.25 0 10-1.5 0v.878A2.25 2.25 0 005.75 8.5h1.5v2.128a2.251 2.251 0 101.5 0V8.5h1.5a2.25 2.25 0 002.25-2.25v-.878a2.25 2.25 0 10-1.5 0v.878a.75.75 0 01-.75.75h-4.5A.75.75 0 015 6.25v-.878zm3.75 7.378a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm3-8.75a.75.75 0 100-1.5.75.75 0 000 1.5z"/>
                            </svg>

                            <!-- Stars -->
                            <g transform="translate(-45, 0)">
                                <text x="12" text-anchor="end" class="stat">%s</text>
                                <svg class="icon" x="-14" y="-11" viewBox="0 0 16 16" width="14" height="14">
                                    <path fill-rule="evenodd" d="M8 .25a.75.75 0 01.673.418l1.882 3.815 4.21.612a.75.75 0 01.416 1.279l-3.046 2.97.719 4.192a.75.75 0 01-1.088.791L8 12.347l-3.766 1.98a.75.75 0 01-1.088-.79l.72-4.194L.818 6.374a.75.75 0 01.416-1.28l4.21-.611L7.327.668A.75.75 0 018 .25z"/>
                                </svg>
                            </g>
                        </g>
                    </g>
                </svg>
                """
                .formatted(
                        REPO_CARD_WIDTH, totalHeight, REPO_CARD_WIDTH, totalHeight,
                        titleColor, textColor, textColor, iconColor,
                        REPO_CARD_WIDTH - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                        headerIconX, headerY - 15,
                        headerTextX, headerY,
                        escapeHtml(repo.name()),
                        descSvg.toString(),
                        footerY - 5,
                        langColor,
                        footerY,
                        escapeHtml(langName),
                        40 + estimateTextWidth(langName) + 15, footerY,
                        commits,
                        footerY,
                        forks,
                        stars);
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

        // (Imagem e Texto)
        int leftCenter = dividerX / 2;
        int imgSize = 125;
        int imgX = 25;
        int imgY = 75;
        int textX = imgX + imgSize + 15;

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
                        textX, escapeHtml(truncate(data.currentTrack().name(), 19)),
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