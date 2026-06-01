package com.example.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
    val blocks = markdown.split("\n\n")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (blocks.all { it.isBlank() }) {
            Text(
                text = "*Empty live markdown preview*",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontStyle = FontStyle.Italic
            )
        } else {
            blocks.forEach { block ->
                val trimmedBlock = block.trim()
                if (trimmedBlock.isNotEmpty()) {
                    RenderMarkdownBlock(trimmedBlock)
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownBlock(block: String) {
    val colorScheme = MaterialTheme.colorScheme

    when {
        // Heading 1
        block.startsWith("# ") -> {
            val text = block.substring(2)
            Text(
                text = parseInlineMarkdown(text),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        // Heading 2
        block.startsWith("## ") -> {
            val text = block.substring(3)
            Text(
                text = parseInlineMarkdown(text),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = colorScheme.secondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        // Heading 3
        block.startsWith("### ") -> {
            val text = block.substring(4)
            Text(
                text = parseInlineMarkdown(text),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = colorScheme.tertiary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        // Blockquote
        block.startsWith(">") -> {
            val lines = block.split("\n").map { line ->
                if (line.startsWith("> ")) line.substring(2) else if (line.startsWith(">")) line.substring(1) else line
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 20.dp)
                        .background(colorScheme.primary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    lines.forEach { line ->
                        Text(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        // Bullet or checklist
        block.startsWith("* ") || block.startsWith("- ") -> {
            val lines = block.split("\n")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEach { line ->
                    val cleanLine = if (line.startsWith("* ")) line.substring(2) else if (line.startsWith("- ")) line.substring(2) else line
                    Row {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(cleanLine),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
        // Numbered List
        block.first().isDigit() && (block.contains(". ")) -> {
            val lines = block.split("\n")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEach { line ->
                    val dotIdx = line.indexOf(". ")
                    val numPrefix = if (dotIdx != -1) line.substring(0, dotIdx + 2) else ""
                    val cleanLine = if (dotIdx != -1) line.substring(dotIdx + 2) else line
                    Row {
                        if (numPrefix.isNotEmpty()) {
                            Text(
                                text = numPrefix,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = parseInlineMarkdown(cleanLine),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
        // Inline code blocks
        block.startsWith("```") && block.endsWith("```") -> {
            val lines = block.split("\n")
            val codeBody = lines.drop(1).dropLast(1).joinToString("\n")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = codeBody,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
        // Standard body paragraph
        else -> {
            Text(
                text = parseInlineMarkdown(block),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * Super lightweight inline formatter that formats:
 * **bold**, *italic*, ~~strikethrough~~, `code` spanning
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0

    while (i < text.length) {
        when {
            // Strong / Bold: **text**
            text.startsWith("**", i) && text.indexOf("**", i + 2) != -1 -> {
                val end = text.indexOf("**", i + 2)
                val innerText = text.substring(i + 2, end)
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(innerText)
                builder.pop()
                i = end + 2
            }
            // Italic: *text*
            text.startsWith("*", i) && text.indexOf("*", i + 1) != -1 -> {
                val end = text.indexOf("*", i + 1)
                val innerText = text.substring(i + 1, end)
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(innerText)
                builder.pop()
                i = end + 1
            }
            // Strikethrough: ~~text~~
            text.startsWith("~~", i) && text.indexOf("~~", i + 2) != -1 -> {
                val end = text.indexOf("~~", i + 2)
                val innerText = text.substring(i + 2, end)
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                builder.append(innerText)
                builder.pop()
                i = end + 2
            }
            // Code block / Inline code: `text`
            text.startsWith("`", i) && text.indexOf("`", i + 1) != -1 -> {
                val end = text.indexOf("`", i + 1)
                val innerText = text.substring(i + 1, end)
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
                builder.append(innerText)
                builder.pop()
                i = end + 1
            }
            // Standard single char
            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
    return builder.toAnnotatedString()
}
