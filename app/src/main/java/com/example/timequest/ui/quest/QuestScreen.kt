package com.example.timequest.ui.quest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.data.Difficulty
import com.example.timequest.data.Progress
import com.example.timequest.data.Quest
import com.example.timequest.data.QuestReward
import com.example.timequest.ui.theme.XpGold
import kotlinx.coroutines.launch

@Composable
fun QuestScreen(
    viewModel: QuestViewModel,
    snackbarHost: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val quests by viewModel.quests.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val scope = rememberCoroutineScope()

    var newTitle by remember { mutableStateOf("") }
    var newDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var editingId by remember { mutableStateOf<String?>(null) }

    // 미완료 퀘스트를 위로, 완료한 퀘스트를 아래로
    val sorted = remember(quests) { quests.filterNot { it.done } + quests.filter { it.done } }
    val doneCount = quests.count { it.done }
    val editing = editingId?.let { id -> quests.firstOrNull { it.id == id } }

    fun toast(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        snackbarHost.currentSnackbarData?.dismiss()
        scope.launch {
            val result = snackbarHost.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun rewardMessage(r: QuestReward): String = when {
        r.leveledUp -> "🎉 레벨 업! Lv.${r.newLevel} 달성 (+${r.xpDelta} XP)"
        r.xpDelta > 0 -> "퀘스트 완료! +${r.xpDelta} XP"
        else -> "${r.xpDelta} XP"
    }

    fun submitNew() {
        if (viewModel.add(newTitle, newDifficulty)) newTitle = ""
    }

    Column(modifier = modifier.fillMaxSize()) {
        LevelCard(
            progress = progress,
            done = doneCount,
            total = quests.size,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        AddBar(
            title = newTitle,
            onTitleChange = { newTitle = it },
            difficulty = newDifficulty,
            onDifficultyChange = { newDifficulty = it },
            onSubmit = ::submitNew,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (doneCount > 0) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    val n = viewModel.clearCompleted()
                    toast("완료한 퀘스트 ${n}개를 정리했어요")
                }) { Text("완료한 퀘스트 정리") }
            }
        }

        if (quests.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorted, key = { it.id }) { quest ->
                    QuestRow(
                        quest = quest,
                        onToggle = { checked ->
                            viewModel.setDone(quest.id, checked)?.let { toast(rewardMessage(it)) }
                        },
                        onClick = { editingId = quest.id },
                        onDelete = {
                            viewModel.delete(quest.id)?.let { (removed, index) ->
                                toast("퀘스트를 삭제했어요", "되돌리기") { viewModel.restore(removed, index) }
                            }
                        },
                    )
                }
            }
        }
    }

    if (editing != null) {
        EditQuestSheet(
            quest = editing,
            onDismiss = { editingId = null },
            onSave = { title, difficulty ->
                viewModel.update(editing.id, title, difficulty)
                editingId = null
            },
            onDelete = {
                val q = editing
                editingId = null
                viewModel.delete(q.id)?.let { (removed, index) ->
                    toast("퀘스트를 삭제했어요", "되돌리기") { viewModel.restore(removed, index) }
                }
            },
        )
    }
}

// ---------------------------------------------------------------- 레벨 카드

@Composable
private fun LevelCard(progress: Progress, done: Int, total: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.xpInLevel / Progress.XP_PER_LEVEL.toFloat(),
        label = "xp",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lv.${progress.level}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = progress.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "오늘의 퀘스트 $done/$total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = XpGold,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${progress.xpInLevel} / ${Progress.XP_PER_LEVEL} XP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ---------------------------------------------------------------- 빠른 추가

@Composable
private fun AddBar(
    title: String,
    onTitleChange: (String) -> Unit,
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("새 퀘스트를 입력하세요") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = MaterialTheme.shapes.large,
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSubmit, enabled = title.isNotBlank()) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Difficulty.entries.forEach { d ->
                FilterChip(
                    selected = difficulty == d,
                    onClick = { onDifficultyChange(d) },
                    label = { Text("${d.emoji} ${d.label}") },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗡️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("아직 퀘스트가 없어요", style = MaterialTheme.typography.titleMedium)
            Text(
                "위 입력창에 할 일을 적고 Enter를 눌러\n첫 퀘스트를 만들어 보세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- 퀘스트 한 줄

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestRow(
    quest: Quest,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    // 끝까지 밀어서 정착(settle)되면 삭제
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // 스와이프를 시작했을 때만 배경을 그린다(카드 뒤로 비쳐 보이지 않게)
            val swiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(
                if (swiping) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "swipeBg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, MaterialTheme.shapes.medium)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (swiping) {
                    Text("삭제", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (quest.done) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = quest.done, onCheckedChange = onToggle)
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (quest.done) TextDecoration.LineThrough else null,
                        color = if (quest.done) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Text(
                    text = "${quest.difficulty.emoji} +${quest.difficulty.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (quest.done) XpGold.copy(alpha = 0.6f) else XpGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 편집 시트

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuestSheet(
    quest: Quest,
    onDismiss: () -> Unit,
    onSave: (String, Difficulty) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(quest.id) { mutableStateOf(quest.title) }
    var difficulty by remember(quest.id) { mutableStateOf(quest.difficulty) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text("퀘스트 수정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text("할 일") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (title.isNotBlank()) onSave(title, difficulty) }),
                maxLines = 3,
                shape = MaterialTheme.shapes.large,
            )

            Spacer(Modifier.height(16.dp))
            Text("난이도", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    DifficultyCard(
                        difficulty = d,
                        selected = difficulty == d,
                        onClick = { difficulty = d },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onSave(title, difficulty) },
                    enabled = title.isNotBlank(),
                ) { Text("저장") }
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(2.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(difficulty.emoji, style = MaterialTheme.typography.headlineSmall)
            Text(difficulty.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("+${difficulty.xp} XP", style = MaterialTheme.typography.labelSmall, color = XpGold)
        }
    }
}
