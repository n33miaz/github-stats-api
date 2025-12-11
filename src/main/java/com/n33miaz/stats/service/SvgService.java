package com.n33miaz.stats.service;

import com.n33miaz.stats.dto.GithubResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SvgService {

    private static final int CARD_WIDTH = 400;
    // private static final int CARD_MIN_HEIGHT = 120;
    private static final int LINE_HEIGHT = 20;
    private static final int MAX_DESC_LINES = 3;
    private static final int MAX_CHARS_PER_LINE = 55;

    public String generateRepoCard(GithubResponse.Repository repo, Map<String, String> colors, boolean hideBorder) {
        // processa as cores (com fallback)
        String titleColor = colors.getOrDefault("title_color", "2f80ed");
        String iconColor = colors.getOrDefault("icon_color", "586069");
        String textColor = colors.getOrDefault("text_color", "434d58");
        String bgColor = colors.getOrDefault("bg_color", "fffefe");
        String borderColor = colors.getOrDefault("border_color", "e4e2e2");

        // processa os dados
        String description = repo.description() != null ? repo.description() : "No description provided";
        String langName = repo.primaryLanguage() != null ? repo.primaryLanguage().name() : "N/A";
        String langColor = repo.primaryLanguage() != null ? repo.primaryLanguage().color() : "#ccc";
        String stars = kFormatter(repo.stargazerCount());
        String forks = kFormatter(repo.forkCount());

        // lógica de quebra de linha
        List<String> descLines = wrapText(description, MAX_CHARS_PER_LINE, MAX_DESC_LINES);
        int descriptionHeight = descLines.size() * LINE_HEIGHT;

        // altura do card baseado no tamanho da descrição
        int totalHeight = (descLines.size() > 1 ? 120 : 110) + descriptionHeight;

        // Y dos ícones de status (Stars/Forks)
        int statusY = totalHeight - 25;

        // descrição
        StringBuilder descSvg = new StringBuilder();
        for (int i = 0; i < descLines.size(); i++) {
            descSvg.append(
                    String.format("<tspan x=\"25\" dy=\"%s\">%s</tspan>", "1.2em", escapeHtml(descLines.get(i))));
        }

        // motagem do SVG 
        return """
                <svg width="%d" height="%d" viewBox="0 0 %d %d" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <style>
                        .header { font: 600 18px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .desc { font: 400 13px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .stat { font: 600 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #%s; }
                        .icon { fill: #%s; }
                    </style>

                    <rect x="0.5" y="0.5" rx="4.5" height="99%%" width="%d" fill="#%s" stroke="#%s" stroke-opacity="%s" />

                    <!-- Header: Icon + Title -->
                    <g transform="translate(25, 35)">
                        <svg class="icon" x="0" y="-13" viewBox="0 0 16 16" version="1.1" width="16" height="16">
                            <path fill-rule="evenodd" d="M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 110-1.5h1.75v-2h-8a1 1 0 00-.714 1.7.75.75 0 01-1.072 1.05A2.495 2.495 0 012 11.5v-9zm10.5-1V9h-8c-.356 0-.694.074-1 .208V2.5a1 1 0 011-1h8zM5 12.25v3.25a.25.25 0 00.4.2l1.45-1.087a.25.25 0 01.3 0L8.6 15.7a.25.25 0 00.4-.2v-3.25a.25.25 0 00-.25-.25h-3.5a.25.25 0 00-.25.25z"></path>
                        </svg>
                        <text x="25" y="0" class="header">%s</text>
                    </g>

                    <!-- Description (Dynamic Wrapping) -->
                    <g transform="translate(0, 45)">
                        <text x="25" y="-5" class="desc">
                            %s
                        </text>
                    </g>

                    <!-- Stats (Lang, Stars, Forks) -->
                    <g transform="translate(30, %d)">
                        <!-- Language -->
                        <g transform="translate(0, 0)">
                            <circle cx="0" cy="-5" r="6" fill="%s" />
                            <text data-testid="lang-name" x="15" class="stat">%s</text>
                        </g>

                        <!-- Stars -->
                        <g transform="translate(%d, 0)">
                            <svg class="icon" y="-12" viewBox="0 0 16 16" version="1.1" width="16" height="16">
                                 <path fill-rule="evenodd" d="M8 .25a.75.75 0 01.673.418l1.882 3.815 4.21.612a.75.75 0 01.416 1.279l-3.046 2.97.719 4.192a.75.75 0 01-1.088.791L8 12.347l-3.766 1.98a.75.75 0 01-1.088-.79l.72-4.194L.818 6.374a.75.75 0 01.416-1.28l4.21-.611L7.327.668A.75.75 0 018 .25z"></path>
                            </svg>
                            <text x="25" class="stat">%s</text>
                        </g>

                        <!-- Forks -->
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
                        CARD_WIDTH, totalHeight, CARD_WIDTH, totalHeight,
                        titleColor, textColor, textColor, iconColor, 
                        CARD_WIDTH - 1, bgColor, borderColor, hideBorder ? "0" : "1", 
                        repo.name(), 
                        descSvg.toString(), 
                        statusY,
                        langColor, langName,
                        estimateTextWidth(langName) + 25, 
                        stars,
                        estimateTextWidth(langName) + estimateTextWidth(stars) + 60,
                        forks
                );
    }

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
                .formatted(text);
    }

    // --- Helpers ---

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
                    if (lastLine.length() > 3) {
                        lines.set(lines.size() - 1, lastLine.substring(0, lastLine.length() - 3) + "...");
                    }
                    return lines;
                }
            }
            if (!currentLine.isEmpty()) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private int estimateTextWidth(String text) {
        return (int) (text.length() * 7.5);
    }

    private String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}