from pathlib import Path
import re

ROOT = Path('presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation')
TARGET = ROOT / 'GlassComponents.kt'


def replace_function(text, name, replacement):
    marker = re.compile(r'(?m)^@Composable\s+(?:private\s+)?fun\s+' + re.escape(name) + r'\s*\(')
    m = marker.search(text)
    if not m:
        raise SystemExit(f'missing function: {name}')
    start = m.start()
    brace = text.find('{', m.end())
    if brace < 0:
        raise SystemExit(f'missing body: {name}')
    depth = 0
    i = brace
    in_str = False
    esc = False
    while i < len(text):
        c = text[i]
        if in_str:
            if esc:
                esc = False
            elif c == '\\':
                esc = True
            elif c == '"':
                in_str = False
        else:
            if c == '"':
                in_str = True
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    end = i + 1
                    return text[:start] + replacement.rstrip() + text[end:]
        i += 1
    raise SystemExit(f'unclosed body: {name}')


def main():
    text = TARGET.read_text(encoding='utf-8')

    text = replace_function(text, 'GlassTopBar', '''@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AccentGradient),
            contentAlignment = Alignment.Center,
        ) { Text("W", color = Color.White, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title.stripHtml(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = GlassTokens.ink)
            if (!subtitle.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(GlassTokens.live))
                    Text(subtitle.glassLabel(), style = MaterialTheme.typography.labelMedium, color = GlassTokens.muted)
                }
            }
        }
        actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = it) }
    }
}''')

    text = replace_function(text, 'GlassCard', '''@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(GlassTokens.radiusLg)
    Surface(
        modifier = modifier.fillMaxWidth().glassMaterial(shape),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
        shadowElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxWidth().background(GlassGradient, shape)) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.18f),
                        ),
                    ),
                    shape,
                ),
            )
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}''')

    text = replace_function(text, 'GlassButton', '''@Composable
fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.heightIn(min = 52.dp).clip(shape).clickable(enabled = enabled, onClick = onClick)
            .background(if (enabled) AccentGradient else Brush.linearGradient(listOf(Color.Gray.copy(alpha = .22f), Color.Gray.copy(alpha = .16f))))
            .glassMaterial(shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .10f), shape))
        Text(label.glassLabel(), Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}''')

    text = replace_function(text, 'GlassOutlinedButton', '''@Composable
fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(15.dp)
    Surface(
        modifier = modifier.heightIn(min = 50.dp).glassMaterial(shape).clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = Color.White.copy(alpha = .34f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(label.glassLabel(), color = GlassTokens.ink, fontWeight = FontWeight.Bold)
        }
    }
}''')

    text = replace_function(text, 'GlassTextField', '''@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) {
    val richText = label == "توضیحات" || label == "توضیح کوتاه"
    val displayValue = if (richText) value.stripHtml() else value
    OutlinedTextField(
        displayValue,
        { onValueChange(if (richText) it.toWooHtml() else it) },
        modifier.fillMaxWidth().heightIn(min = 56.dp).glassMaterial(RoundedCornerShape(16.dp)),
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = .34f),
            focusedContainerColor = Color.White.copy(alpha = .50f),
            unfocusedBorderColor = GlassTokens.glassBorder,
            focusedBorderColor = GlassTokens.accent,
            cursorColor = GlassTokens.accent,
        ),
    )
}''')

    text = replace_function(text, 'GlassSearchField', '''@Composable
fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) =
    OutlinedTextField(
        value, onValueChange,
        modifier.fillMaxWidth().heightIn(min = 56.dp).glassMaterial(RoundedCornerShape(17.dp)),
        singleLine = true,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        trailingIcon = { if (value.isNotEmpty()) GlassTextButton("پاک", { onClear?.invoke() ?: onValueChange("") }) },
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = .38f),
            focusedContainerColor = Color.White.copy(alpha = .54f),
            unfocusedBorderColor = GlassTokens.glassBorder,
            focusedBorderColor = GlassTokens.accent,
        ),
    )''')

    TARGET.write_text(text, encoding='utf-8')

if __name__ == '__main__':
    main()
