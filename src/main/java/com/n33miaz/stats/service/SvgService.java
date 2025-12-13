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

    // --- CARD DE REPOSITÓRIO (PIN) ---
    public String generateRepoCard(GithubResponse.Repository repo, Map<String, String> colors,
            boolean hideBorder,
            boolean showDescription) {
        // cores
        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String iconColor = colors.getOrDefault("icon_color", "586069");
        String textColor = colors.getOrDefault("text_color", "434d58");
        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        // dados
        String langName = repo.primaryLanguage() != null ? repo.primaryLanguage().name() : "N/A";
        String langColor = repo.primaryLanguage() != null ? repo.primaryLanguage().color() : "#ccc";
        String stars = kFormatter(repo.stargazerCount());
        String forks = kFormatter(repo.forkCount());

        int commitCount = (repo.object() != null && repo.object().history() != null)
                ? repo.object().history().totalCount()
                : 0;
        String commits = kFormatter(commitCount);

        // descrição
        String rawDescription = repo.description();
        boolean hasDescriptionText = rawDescription != null && !rawDescription.isEmpty();
        boolean shouldRenderDesc = showDescription && hasDescriptionText;

        List<String> descLines = new ArrayList<>();
        if (shouldRenderDesc) {
            descLines = wrapText(rawDescription, REPO_MAX_CHARS_PER_LINE, REPO_MAX_DESC_LINES);
        }

        int headerFontSize = shouldRenderDesc ? 18 : 22;
        int paddingTop = 40;
        int headerHeight = 25;
        int descLineHeight = 22;

        int descBlockHeight = shouldRenderDesc ? Math.max(descLines.size() * descLineHeight, 20) : 0;

        int footerHeight = 40;
        int paddingBottom = 15;

        int extraSpacing = shouldRenderDesc ? 0 : 25;

        int totalHeight = paddingTop + headerHeight + descBlockHeight + footerHeight + paddingBottom + extraSpacing;

        int headerY = shouldRenderDesc ? 35 : (totalHeight - footerHeight) / 2 - 5;

        int descStartY = headerY + 30;
        int footerY = totalHeight - 20;

        int titleWidth = estimateTextWidth(repo.name());
        if (!shouldRenderDesc)
            titleWidth = (int) (titleWidth * 1.2);

        // descrição
        StringBuilder descSvg = new StringBuilder();
        if (shouldRenderDesc) {
            for (int i = 0; i < descLines.size(); i++) {
                int lineY = descStartY + (i * descLineHeight);
                descSvg.append(String.format(
                        "<text x=\"200\" y=\"%d\" text-anchor=\"middle\" class=\"desc\">%s</text>",
                        lineY,
                        escapeHtml(descLines.get(i))));
            }
        }

        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <style>
                        .header { font: 700 %dpx 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
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
                        <text x="200" y="%d" text-anchor="middle" class="header">%s</text>
                    </g>

                    <!-- DESCRIÇÃO -->
                    <g class="fade-in d-2">
                        %s
                    </g>

                    <!-- FOOTER -->
                    <g class="fade-in d-3">
                        <!-- ESQUERDA -->
                        <circle cx="30" cy="%d" r="5" fill="%s" />
                        <text x="40" y="%d" class="stat">%s</text>

                        <g transform="translate(%d, %d)">
                            <svg class="icon" y="-11" viewBox="0 0 16 16" width="14" height="14">
                                <path fill-rule="evenodd" d="M10.5 7.75a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0zm1.43.75a4.002 4.002 0 01-7.86 0H.75a.75.75 0 110-1.5h3.32a4.001 4.001 0 017.86 0h3.32a.75.75 0 110 1.5h-3.32z"/>
                            </svg>
                            <text x="18" class="stat">%s</text>
                        </g>

                        <!-- DIREITA -->
                        <g transform="translate(365, %d)">
                            <text x="10" text-anchor="end" class="stat">%s</text>
                            <svg class="icon" x="-15" y="-11" viewBox="0 0 16 16" width="14" height="14">
                                <path fill-rule="evenodd" d="M5 3.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm0 2.122a2.25 2.25 0 10-1.5 0v.878A2.25 2.25 0 005.75 8.5h1.5v2.128a2.251 2.251 0 101.5 0V8.5h1.5a2.25 2.25 0 002.25-2.25v-.878a2.25 2.25 0 10-1.5 0v.878a.75.75 0 01-.75.75h-4.5A.75.75 0 015 6.25v-.878zm3.75 7.378a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm3-8.75a.75.75 0 100-1.5.75.75 0 000 1.5z"/>
                            </svg>

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
                        headerFontSize, titleColor, textColor, textColor, iconColor,
                        REPO_CARD_WIDTH - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                        headerY,
                        escapeHtml(repo.name()),
                        descSvg.toString(),
                        footerY - 5, langColor, footerY, escapeHtml(langName),
                        40 + estimateTextWidth(langName) + 15, footerY, commits,
                        footerY, forks, stars);
    }

    // --- CARD DE ESTATÍSTICAS ---
    public String generateStatsCard(com.n33miaz.stats.service.GithubService.StatsData stats, Map<String, String> colors,
            boolean hideBorder) {
        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String iconColor = colors.getOrDefault("icon_color", "4c71f2");
        String textColor = colors.getOrDefault("text_color", "434d58");
        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        int width = 450;
        int height = 195;
        int paddingX = 25;
        int lineHeight = 28;

        // ícones
        String iconCommits = "M1.643 3.143L.427 1.927A.25.25 0 000 2.104V5.75c0 .138.112.25.25.25h3.646a.25.25 0 00.177-.427L2.715 4.215a6.5 6.5 0 11-1.18 4.458.75.75 0 10-1.493.154 8.001 8.001 0 101.6-5.684zM7.75 4a.75.75 0 01.75.75v2.992l2.028.812a.75.75 0 01-.557 1.392l-2.5-1A.75.75 0 017 8.25v-3.5A.75.75 0 017.75 4z";
        String iconPRs = "M7.177 3.073L9.573.677A.25.25 0 0110 .854v4.792a.25.25 0 01-.427.177L7.177 3.427a.25.25 0 010-.354zM3.75 2.5a.75.75 0 100 1.5.75.75 0 000-1.5zm-2.25.75a2.25 2.25 0 113 2.122v5.256a2.251 2.251 0 11-1.5 0V5.372A2.25 2.25 0 011.5 3.25zM11 2.5h-1V4h1a1 1 0 011 1v5.628a2.251 2.251 0 101.5 0V5A2.5 2.5 0 0011 2.5zm1 10.25a.75.75 0 111.5 0 .75.75 0 01-1.5 0zM3.75 12a.75.75 0 100 1.5.75.75 0 000-1.5z";
        String iconIssues = "M8 1.5a6.5 6.5 0 100 13 6.5 6.5 0 000-13zM0 8a8 8 0 1116 0A8 8 0 010 8zm9 3a1 1 0 11-2 0 1 1 0 012 0zm-.25-6.25a.75.75 0 00-1.5 0v3.5a.75.75 0 001.5 0v-3.5z";
        String iconContribs = "M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 110-1.5h1.75v-2h-8a1 1 0 00-.714 1.7.75.75 0 01-1.072 1.05A2.495 2.495 0 012 11.5v-9zm10.5-1V9h-8c-.356 0-.694.074-1 .208V2.5a1 1 0 011-1h8zM5 12.25v3.25a.25.25 0 00.4.2l1.45-1.087a.25.25 0 01.3 0L8.6 15.7a.25.25 0 00.4-.2v-3.25a.25.25 0 00-.25-.25h-3.5a.25.25 0 00-.25.25z";

        StringBuilder rowsSvg = new StringBuilder();
        int currentY = 0;

        rowsSvg.append(createStatRow(0, currentY, iconCommits, "Total Commits", String.valueOf(stats.commits()),
                iconColor, textColor));
        currentY += lineHeight;

        rowsSvg.append(createStatRow(0, currentY, iconContribs, "Total Contributions",
                kFormatter(stats.contributedTo()), iconColor, textColor));
        currentY += lineHeight;

        rowsSvg.append(createStatRow(0, currentY, iconPRs, "Total Pull Requests", kFormatter(stats.prs()), iconColor,
                textColor));
        currentY += lineHeight;

        rowsSvg.append(createStatRow(0, currentY, iconIssues, "Total Issues", kFormatter(stats.issues()), iconColor,
                textColor));

        // rank
        double radius = 40;
        double circumference = Math.PI * (radius * 2);
        double rankPercent = stats.rank().percentile();
        double strokeOffset = ((100 - rankPercent) / 100) * circumference;
        String ringColor = titleColor;

        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <style>
                        .header { font: 600 18px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; animation: fadeIn 0.8s ease-in-out forwards; }
                        .stat-label { font: 600 14px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .stat-value { font: 600 14px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .icon { fill: #%s; }
                        .rank-text { font: 800 24px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; dominant-baseline: central; text-anchor: middle; }

                        .fade-in { opacity: 0; animation: fadeIn 0.5s ease-in-out forwards; }

                        .delay-1 { animation-delay: 0.1s; }
                        .delay-2 { animation-delay: 0.2s; }
                        .delay-3 { animation-delay: 0.3s; }
                        .delay-4 { animation-delay: 0.4s; }
                        .delay-5 { animation-delay: 0.5s; }

                        @keyframes fadeIn {
                            from { opacity: 0; transform: translateX(-10px); }
                            to { opacity: 1; transform: translateX(0); }
                        }

                        .rank-circle {
                            stroke-dasharray: 250;
                            stroke-dashoffset: 250;
                            animation: animateRank 1s ease-in-out forwards 0.5s;
                        }
                        @keyframes animateRank { to { stroke-dashoffset: %f; } }
                    </style>

                    <!-- Fundo -->
                    <rect x="0.5" y="0.5" rx="10" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                    <!-- Stats -->
                    <g transform="translate(%d, %d)">
                        %s
                    </g>

                    <!-- Rank -->
                    <g transform="translate(%d, %d)">
                        <g class="fade-in delay-5">
                            <circle cx="40" cy="40" r="40" fill="none" stroke="#%s" stroke-width="6" opacity="0.2" />
                            <circle cx="40" cy="40" r="40" fill="none" stroke="#%s" stroke-width="6" class="rank-circle" stroke-linecap="round" transform="rotate(-90 40 40)" />
                            <text x="40" y="40" class="rank-text">%s</text>
                        </g>
                    </g>
                </svg>
                """
                .formatted(
                        width, height, width, height,
                        titleColor, textColor, textColor, iconColor, textColor,
                        strokeOffset,
                        width - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                        paddingX, 55,
                        rowsSvg.toString(),
                        width - 100 - paddingX, (height / 2) - 40,
                        ringColor, ringColor,
                        stats.rank().level());
    }

    private String createStatRow(int x, int y, String iconPath, String label, String valueStr, String iconColor,
            String textColor) {
        int delayIndex = (y / 28) + 1;

        return """
                <g transform="translate(%d, %d)">
                    <g class="fade-in delay-%d">
                        <svg class="icon" x="0" y="-10" viewBox="0 0 16 16" width="16" height="16">
                            <path fill-rule="evenodd" d="%s"/>
                        </svg>
                        <text x="25" y="3" class="stat-label">%s:</text>
                        <text x="200" y="3" class="stat-value">%s</text>
                    </g>
                </g>
                """.formatted(x, y, delayIndex, iconPath, label, valueStr);
    }

    // --- CARD DE STREAK ---
    public String generateStreakCard(
            com.n33miaz.stats.dto.StreakStatsDto stats,
            Map<String, String> colors,
            boolean hideBorder) {

        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String ringColor = colors.getOrDefault("ring", titleColor);
        String fireColor = colors.getOrDefault("fire", titleColor);
        String currStreakNumColor = colors.getOrDefault("currStreakNum", titleColor);
        String sideNumsColor = colors.getOrDefault("sideNums", titleColor);

        String textColor = colors.getOrDefault("text_color", "434d58");
        String sideLabelsColor = colors.getOrDefault("sideLabels", textColor);
        String datesColor = colors.getOrDefault("dates", textColor);

        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        int width = 450;
        int height = 195;

        int col1X = 75;
        int col2X = 225;
        int col3X = 375;

        String fireIcon = "M 8.5 2.5 c 0 0 -2 2 -2 4.5 c 0 1.5 1 2.5 1 2.5 c 0 0 -2 -0.5 -3 -3 c -0.5 1 -1 2.5 -0.5 4 c 0.5 2 2.5 3.5 5 3 c 2 -0.5 3 -2.5 2.5 -4.5 c -0.5 -1.5 -2 -2.5 -3 -2.5 c 0 0 1 -0.5 1.5 -1 c 0.5 -0.5 0.5 -1.5 0.5 -1.5 c 0 0 -1 0 -2 -1.5 Z";

        return String.format(java.util.Locale.US,
                """
                        <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <style>
                                .stat-val { font: 700 30px 'Segoe UI', Ubuntu, Sans-Serif; }
                                .stat-lbl { font: 600 14px 'Segoe UI', Ubuntu, Sans-Serif; }
                                .stat-dte { font: 400 12px 'Segoe UI', Ubuntu, Sans-Serif; }

                                .fade-in { opacity: 0; animation: fadeIn 0.8s ease-in-out forwards; }
                                .fire-anim { animation: firePulse 2s ease-in-out infinite alternate; transform-origin: center; }

                                @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
                                @keyframes firePulse { 0%% { opacity: 0.8; transform: scale(1); } 100%% { opacity: 1; transform: scale(1.1); } }
                            </style>

                            <!-- Fundo -->
                            <rect x="0.5" y="0.5" rx="10" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                            <!-- ESQUERDA -->
                            <g class="fade-in" style="animation-delay: 0.1s">
                                <text x="%d" y="82" text-anchor="middle" class="stat-val" fill="#%s">%s</text>
                                <text x="%d" y="108" text-anchor="middle" class="stat-lbl" fill="#%s">Commits</text>
                                <text x="%d" y="128" text-anchor="middle" class="stat-dte" fill="#%s" opacity="0.8">Current Year</text>
                            </g>

                            <!-- DIVISOR -->
                            <line x1="150" y1="40" x2="150" y2="155" stroke="#%s" stroke-width="1" stroke-opacity="0.2" />

                            <!-- MEIO -->
                            <g class="fade-in" style="animation-delay: 0.2s">
                                <circle cx="%d" cy="62" r="38" fill="none" stroke="#%s" stroke-width="4" stroke-opacity="0.2" />
                                <circle cx="%d" cy="62" r="38" fill="none" stroke="#%s" stroke-width="4" stroke-dasharray="240" stroke-dashoffset="60" stroke-linecap="round" transform="rotate(-90 %d 62)"/>

                                <g transform="translate(%d, 42) scale(1.5)">
                                    <path d="%s" fill="#%s" class="fire-anim" />
                                </g>

                                <text x="%d" y="72" text-anchor="middle" class="stat-val" fill="#%s">%d</text>

                                <text x="%d" y="132" text-anchor="middle" class="stat-lbl" fill="#%s">Current Streak</text>
                                <text x="%d" y="152" text-anchor="middle" class="stat-dte" fill="#%s" opacity="0.8">%s</text>
                            </g>

                            <!-- DIVISOR -->
                            <line x1="300" y1="40" x2="300" y2="155" stroke="#%s" stroke-width="1" stroke-opacity="0.2" />

                            <!-- DIREITA -->
                            <g class="fade-in" style="animation-delay: 0.3s">
                                <text x="%d" y="82" text-anchor="middle" class="stat-val" fill="#%s">%d</text>
                                <text x="%d" y="108" text-anchor="middle" class="stat-lbl" fill="#%s">Longest Streak</text>
                                <text x="%d" y="128" text-anchor="middle" class="stat-dte" fill="#%s" opacity="0.8">%s</text>
                            </g>

                        </svg>
                        """,
                width, height, width, height,
                width - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                col1X, sideNumsColor, kFormatter(stats.currentYearCommits()),
                col1X, sideLabelsColor,
                col1X, datesColor,
                textColor,
                col2X, ringColor,
                col2X, ringColor, col2X,
                col2X - 12, fireIcon, fireColor,
                col2X, currStreakNumColor, stats.currentStreak(),
                col2X, ringColor,
                col2X, datesColor, stats.currentStreakRange(),
                textColor,
                col3X, sideNumsColor, stats.longestStreak(),
                col3X, sideLabelsColor,
                col3X, datesColor, stats.longestStreakRange());
    }

    // --- GRÁFICO DE CONTRIBUIÇÃO (GitHub + WakaTime) ---
    public String generateContributionGraph(
            com.n33miaz.stats.dto.GithubContributionResponse githubData,
            com.n33miaz.stats.dto.WakaTimeSummaryResponse wakaData,
            Map<String, String> colors,
            boolean hideBorder,
            String username) {

        // cores
        String titleColor = colors.getOrDefault("title_color", "762075");
        String textColor = colors.getOrDefault("text_color", "c9d1d9");
        String bgColor = colors.getOrDefault("bg_color", "0d1117");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");
        String wakaColor = "39d353";

        // dados
        List<DailyStat> stats = mergeData(githubData, wakaData, 7);

        int maxCommits = stats.stream().mapToInt(DailyStat::commits).max().orElse(5);
        maxCommits = Math.max(maxCommits, 5);
        double maxSeconds = stats.stream().mapToDouble(DailyStat::seconds).max().orElse(3600.0);
        maxSeconds = Math.max(maxSeconds, 3600.0);

        // layout
        int width = 800;
        int height = 300;
        int padTop = 60;
        int padBottom = 50;
        int padLeft = 60;
        int padRight = 60;

        int graphHeight = height - padTop - padBottom;
        int graphWidth = width - padLeft - padRight;
        double stepX = (double) graphWidth / (stats.size() - 1);

        // grid
        StringBuilder gridSvg = new StringBuilder();
        StringBuilder labelsSvg = new StringBuilder();

        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            int y = padTop + graphHeight - (int) (graphHeight * ratio);

            // grid horizontal
            gridSvg.append(String.format(java.util.Locale.US,
                    "<line x1='%d' y1='%d' x2='%d' y2='%d' class='grid' />",
                    padLeft, y, width - padRight, y));

            // labels
            int valCommit = (int) (maxCommits * ratio);
            String valTime = formatDurationShort((long) (maxSeconds * ratio));

            labelsSvg.append(String.format(java.util.Locale.US,
                    "<text x='%d' y='%d' text-anchor='end' class='axis-text'>%d</text>",
                    padLeft - 10, y + 4, valCommit));

            labelsSvg.append(String.format(java.util.Locale.US,
                    "<text x='%d' y='%d' text-anchor='start' class='axis-text'>%s</text>",
                    width - padRight + 10, y + 4, valTime));
        }

        // pontos e tooltips
        List<Point> commitPoints = new ArrayList<>();
        List<Point> wakaPoints = new ArrayList<>();
        StringBuilder xAxisSvg = new StringBuilder();
        StringBuilder pointsAndTooltipsSvg = new StringBuilder();

        for (int i = 0; i < stats.size(); i++) {
            DailyStat stat = stats.get(i);

            double x = padLeft + (i * stepX);
            double yCommits = padTop + graphHeight - ((double) stat.commits / maxCommits * graphHeight);
            double yWaka = padTop + graphHeight - (stat.seconds / maxSeconds * graphHeight);

            commitPoints.add(new Point(x, yCommits));
            wakaPoints.add(new Point(x, yWaka));

            // grid vertical
            if (i > 0 && i < stats.size() - 1) {
                gridSvg.append(String.format(java.util.Locale.US,
                        "<line x1='%.2f' y1='%d' x2='%.2f' y2='%d' class='grid' />",
                        x, padTop, x, height - padBottom));
            }

            // Datas
            String textAnchor = (i == 0) ? "start" : (i == stats.size() - 1) ? "end" : "middle";
            xAxisSvg.append(String.format(java.util.Locale.US,
                    "<text x='%.2f' y='%d' text-anchor='%s' class='axis-text'>%s</text>",
                    x, height - 15, textAnchor, formatDateDDMM(stat.date)));

            // colisão/sobreposição
            double dist = Math.abs(yCommits - yWaka);
            boolean overlap = dist < 12;
            boolean hasWaka = stat.seconds > 0;
            String dateFormatted = formatDateDDMM(stat.date);

            String timeTooltip = formatDurationFull((long) stat.seconds);

            String tooltipAnchor = "middle";
            if (i == 0)
                tooltipAnchor = "start";
            if (i == stats.size() - 1)
                tooltipAnchor = "end";

            if (overlap && hasWaka) {
                // pontos fundidos
                pointsAndTooltipsSvg.append(generateInteractivePoint(
                        x, yCommits,
                        "ffffff", bgColor,
                        titleColor, wakaColor,
                        stat.commits, timeTooltip, dateFormatted,
                        tooltipAnchor, true, i));
            } else {
                // pontos separados
                if (hasWaka) {
                    pointsAndTooltipsSvg.append(generateInteractivePoint(
                            x, yWaka,
                            wakaColor, bgColor,
                            null, wakaColor,
                            -1, timeTooltip, dateFormatted,
                            tooltipAnchor, false, i));
                }
                pointsAndTooltipsSvg.append(generateInteractivePoint(
                        x, yCommits,
                        titleColor, bgColor,
                        titleColor, null,
                        stat.commits, null, dateFormatted,
                        tooltipAnchor, false, i));
            }
        }

        // paths suaves
        String commitsLinePath = buildSmoothPath(commitPoints, false, 0, 0, 0);
        String commitsAreaPath = buildSmoothPath(commitPoints, true, height - padBottom, padLeft, width - padRight);
        String wakaLinePath = buildSmoothPath(wakaPoints, false, 0, 0, 0);

        // montagem
        return String.format(java.util.Locale.US,
                """
                        <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <style>
                                .title { font: 700 20px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                                .axis-text { font: 400 11px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; opacity: 0.7; }
                                .legend { font: 600 12px 'Segoe UI', Ubuntu, Sans-Serif; }

                                .grid { stroke: #%s; stroke-width: 1; stroke-opacity: 0.3; stroke-dasharray: 2px; }

                                .line-path { stroke-dasharray: 5000; stroke-dashoffset: 5000; animation: dash 3.5s ease-in-out forwards; }
                                .area-path { opacity: 0; animation: fadeIn 1.5s ease-out forwards 0.5s; }

                                .point-anim { opacity: 0; transform-origin: center; animation: blink 0.8s ease-in-out forwards; }

                                @keyframes dash { to { stroke-dashoffset: 0; } }
                                @keyframes fadeIn { to { opacity: 1; } }
                                @keyframes blink { from { opacity: 0; transform: scale(0); } to { opacity: 1; transform: scale(1); } }

                                /* TOOLTIP */
                                .point-group { cursor: pointer; }
                                .tooltip-container { opacity: 0; transition: opacity 0.2s ease-in-out; pointer-events: none; }
                                .point-group:hover .tooltip-container { opacity: 1; }
                                .point-group:hover .visible-point { stroke-width: 4px; filter: drop-shadow(0 0 5px rgba(0,0,0,0.5)); }

                                .tooltip-box { fill: #0d1117; stroke: #30363d; stroke-width: 1px; rx: 4; filter: drop-shadow(0 4px 6px rgba(0,0,0,0.3)); }
                                .tooltip-header { font: 600 11px 'Segoe UI', Ubuntu, Sans-Serif; fill: #c9d1d9; }
                                .tooltip-text { font: 400 10px 'Segoe UI', Ubuntu, Sans-Serif; fill: #8b949e; }
                            </style>

                            <rect x="0.5" y="0.5" rx="10" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                            <!-- Header -->
                            <g transform="translate(%d, 40)">
                                <text x="0" y="0" class="title">Weekly Activity</text>
                            </g>

                            <!-- Legenda -->
                            <g transform="translate(%d, 40)">
                                <rect x="0" y="-8" width="10" height="10" rx="2" fill="#%s" />
                                <text x="15" y="1" class="legend" fill="#%s">Commits</text>
                                <rect x="80" y="-8" width="10" height="10" rx="2" fill="#%s" />
                                <text x="95" y="1" class="legend" fill="#%s">Coding Time</text>
                            </g>

                            <!-- Eixos e Grids -->
                            %s
                            %s
                            %s

                            <!-- Gráficos -->
                            <defs>
                                <linearGradient id="gradCommits" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%%" stop-color="#%s" stop-opacity="0.2"/>
                                    <stop offset="100%%" stop-color="#%s" stop-opacity="0"/>
                                </linearGradient>
                            </defs>

                            <path d="%s" fill="url(#gradCommits)" class="area-path" />
                            <path d="%s" fill="none" stroke="#%s" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="line-path" style="animation-delay: 0.2s"/>
                            <path d="%s" fill="none" stroke="#%s" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" class="line-path"/>

                            <!-- Pontos com Tooltip -->
                            %s
                        </svg>
                        """,
                width, height, width, height,
                titleColor, textColor, titleColor,
                width - 1, bgColor, borderColor, hideBorder ? "0" : "1",
                padLeft,
                width - padRight - 170,
                titleColor, textColor,
                wakaColor, textColor,
                gridSvg.toString(), labelsSvg.toString(), xAxisSvg.toString(),
                titleColor, titleColor,
                commitsAreaPath,
                wakaLinePath, wakaColor,
                commitsLinePath, titleColor,
                pointsAndTooltipsSvg.toString());
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

        int leftCenter = dividerX / 2;
        int imgSize = 110; 
        int imgX = 25;
        int imgY = 50;    
        int textX = imgX + imgSize + 15;
        
        int bottomY = 180; 

        String coverImage = data.currentTrack().imageBase64().isEmpty()
                ? renderDefaultDisk(imgX + (imgSize / 2), imgY + (imgSize / 2), imgSize / 2)
                : String.format(
                        "<image x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" href=\"%s\" clip-path=\"url(#clip-main)\" class=\"album-art\"/>",
                        imgX, imgY, imgSize, imgSize, data.currentTrack().imageBase64());

        String statusText = data.currentTrack().isPlaying() ? "NOW PLAYING" : "LAST PLAYED";

        // Equalizador
        String equalizer = data.currentTrack().isPlaying() ? renderEqualizer(iconColor, textX+10, 35) : "";

        // Plays 
        String playsBadge = data.currentTrack().userPlayCount() > 0
                ? String.format(
                        """
                                  <g transform="translate(%d, %d)">
                                      <rect x="-50" y="0" width="100" height="20" rx="10" fill="#%s" fill-opacity="0.15"/>
                                      <text x="0" y="14" text-anchor="middle" font-size="10" font-weight="bold" fill="#%s">%d plays</text>
                                  </g>
                                """,
                        leftCenter, bottomY, titleColor, titleColor, data.currentTrack().userPlayCount())
                : "";

        // Listas
        String artistsList = renderList(data.topArtists(), col1X, 50, true, textColor, iconColor, false);
        String albumsList = renderList(data.topAlbums(), col2X, 50, false, textColor, iconColor, true);

        // Período
        int rightCenter = dividerX + (800 - dividerX) / 2;
        String periodBadge = String.format("""
                    <g transform="translate(%d, %d)">
                        <rect x="-40" y="0" width="80" height="20" rx="10" fill="#%s" fill-opacity="0.15"/>
                        <text x="0" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#%s">%s</text>
                    </g>
                """, rightCenter, bottomY, titleColor, titleColor, periodText);

        return """
                    <svg width="800" height="215" viewBox="0 0 800 215" fill="none" xmlns="http://www.w3.org/2000/svg">
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
                            <!-- Status -->
                            <text x="%d" y="30" text-anchor="middle" class="section-header">%s</text>

                            <!-- Imagem -->
                            %s

                            <!-- Track Info -->
                            <text x="%d" y="90" text-anchor="start" class="title">%s</text>
                            <text x="%d" y="115" text-anchor="start" class="subtitle">%s</text>

                            %s <!-- Equalizer -->
                            %s <!-- Plays -->
                        </g>

                        <!-- DIVISOR -->
                        <line x1="%d" y1="30" x2="%d" y2="%d" stroke="#%s" stroke-width="1" stroke-opacity="0.2" />

                        <!-- DIREITA -->
                        <g class="fade-in" style="animation-delay: 0.3s">
                            <text x="%d" y="30" text-anchor="middle" class="section-header">Top Artists</text>
                            %s
                        </g>

                        <g class="fade-in" style="animation-delay: 0.5s">
                            <text x="%d" y="30" text-anchor="middle" class="section-header">Top Albums</text>
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
                        textX, escapeHtml(truncate(data.currentTrack().name(), 15)),
                        textX, escapeHtml(truncate(data.currentTrack().artist(), 25)),
                        equalizer, playsBadge,
                        dividerX, dividerX, bottomY, textColor,
                        col1X + 90, artistsList,
                        col2X + 90, albumsList,
                        periodBadge);
    }

    // --- HELPERS ---
    private List<DailyStat> mergeData(
            com.n33miaz.stats.dto.GithubContributionResponse gh,
            com.n33miaz.stats.dto.WakaTimeSummaryResponse wk,
            int days) {

        java.util.TreeMap<String, DailyStat> map = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter iso = java.time.format.DateTimeFormatter.ISO_DATE;
        java.time.LocalDate end = java.time.LocalDate.now();

        java.time.LocalDate start = end.minusDays(days - 1);

        start.datesUntil(end.plusDays(1)).forEach(d -> {
            map.put(d.format(iso), new DailyStat(d.format(iso), 0, 0.0));
        });

        // dados do GitHub
        if (gh != null && gh.data() != null && gh.data().user() != null) {
            gh.data().user().contributionsCollection().contributionCalendar().weeks().forEach(week -> {
                week.contributionDays().forEach(day -> {
                    if (map.containsKey(day.date())) {
                        DailyStat current = map.get(day.date());
                        map.put(day.date(), new DailyStat(day.date(), day.contributionCount(), current.seconds));
                    }
                });
            });
        }

        // dados do WakaTime
        if (wk != null && wk.data() != null) {
            wk.data().forEach(summary -> {
                String date = summary.range().date();
                if (map.containsKey(date)) {
                    DailyStat current = map.get(date);
                    map.put(date, new DailyStat(date, current.commits, summary.grandTotal().totalSeconds()));
                }
            });
        }

        return new ArrayList<>(map.values());
    }

    private String formatDurationShort(long totalSeconds) {
        if (totalSeconds == 0)
            return "0m";
        long hours = totalSeconds / 3600;
        if (hours > 0)
            return hours + "h";
        long minutes = (totalSeconds % 3600) / 60;
        return minutes + "m";
    }

    private String formatDurationFull(long totalSeconds) {
        if (totalSeconds == 0)
            return "0m";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        return minutes + "m";
    }

    private record DailyStat(String date, int commits, double seconds) {
    }

    private String formatDateDDMM(String isoDate) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
            return String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
        } catch (Exception e) {
            return isoDate;
        }
    }

    private record Point(double x, double y) {
    }

    private String buildSmoothPath(List<Point> points, boolean closePath, int closeY, int closeXStart, int closeXEnd) {
        if (points.isEmpty())
            return "";

        StringBuilder path = new StringBuilder();
        path.append(String.format(java.util.Locale.US, "M %.2f %.2f", points.get(0).x, points.get(0).y));

        for (int i = 0; i < points.size() - 1; i++) {
            Point p0 = (i > 0) ? points.get(i - 1) : points.get(0);
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            Point p3 = (i < points.size() - 2) ? points.get(i + 2) : p2;

            double tension = 0.2;

            double cp1x = p1.x + (p2.x - p0.x) * tension;
            double cp1y = p1.y + (p2.y - p0.y) * tension;
            double cp2x = p2.x - (p3.x - p1.x) * tension;
            double cp2y = p2.y - (p3.y - p1.y) * tension;

            path.append(String.format(java.util.Locale.US,
                    " C %.2f %.2f, %.2f %.2f, %.2f %.2f",
                    cp1x, cp1y, cp2x, cp2y, p2.x, p2.y));
        }

        if (closePath) {
            path.append(String.format(java.util.Locale.US, " L %.2f %d L %d %d Z",
                    (double) closeXEnd, closeY, closeXStart, closeY));
        }

        return path.toString();
    }

    private String generateInteractivePoint(
            double x, double y,
            String pointColor, String strokeColor,
            String commitTextColor, String wakaTextColor,
            int commits, String time, String date,
            String anchor, boolean isMerged, int index) {

        StringBuilder tooltipContent = new StringBuilder();

        int currentY = 18;

        // cabeçalho (Data)
        tooltipContent.append(String.format(
                "<text x='0' y='%d' text-anchor='middle' class='tooltip-header' font-weight='bold'>%s</text>",
                currentY, date));

        currentY += 20;

        // linha de commits
        if (commits >= 0) {
            tooltipContent.append(String.format(
                    "<text x='0' y='%d' text-anchor='middle' class='tooltip-text' fill='#%s'>Commits: %d</text>",
                    currentY, commitTextColor != null ? commitTextColor : "c9d1d9", commits));
            currentY += 16;
        }

        // linha de tempo
        if (time != null) {
            tooltipContent.append(String.format(
                    "<text x='0' y='%d' text-anchor='middle' class='tooltip-text' fill='#%s'>Time: %s</text>",
                    currentY, wakaTextColor != null ? wakaTextColor : "c9d1d9", time));
            currentY += 16;
        }

        // --- CÁLCULOS ---
        int boxWidth = 125;
        int boxHeight = currentY - 6;
        int boxY = -(boxHeight + 12);

        int xOffset = 0;
        if ("start".equals(anchor))
            xOffset = 45;
        if ("end".equals(anchor))
            xOffset = -45;

        if (y < boxHeight + 20) {
            boxY = 20;
        }

        return String.format(java.util.Locale.US,
                """
                        <g class="point-group" transform="translate(%.2f, %.2f)">
                            <!-- Hitbox invisível maior -->
                            <circle cx="0" cy="0" r="15" fill="transparent" />

                            <!-- Ponto visível -->
                            <circle cx="0" cy="0" r="5" fill="#%s" stroke="#%s" stroke-width="2" class="visible-point point-anim" style="animation-delay: %.2fs" />

                            <!-- Tooltip Container -->
                            <g class="tooltip-container" transform="translate(%d, %d)">
                                <!-- Fundo do Modal -->
                                <rect x="%d" y="0" width="%d" height="%d" class="tooltip-box" />

                                <!-- Textos (Já posicionados com Y absoluto) -->
                                %s
                            </g>
                        </g>
                        """,
                x, y,
                pointColor, strokeColor, 1.0 + (index * 0.1),
                xOffset, boxY,
                -boxWidth / 2, boxWidth, boxHeight,
                tooltipContent.toString());
    }

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

            y += 42;
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