package com.example.faithquiz.ui.view.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.Dimensions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mistakes by ProgressDataStore.observeMistakes(context).collectAsState(initial = emptyList())
    val dueList by ProgressDataStore.observeDueReview(context).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.review)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_description))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimensions.screenPadding)
        ) {
            // Overview
            Text("Smart Review (SRS)", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "We schedule your mistakes for spaced repetition. Do the due items first, then browse all past mistakes below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(Dimensions.spaceSmall))

            // Due panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(Dimensions.paddingMedium)) {
                    Text("Due Now", style = MaterialTheme.typography.titleMedium)
                    Text("${dueList.size} items scheduled today", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(Dimensions.spaceSmall))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = dueList.isNotEmpty(), onClick = { /* hook up inline review flow later */ }) { Text("Start Review") }
                        OutlinedButton(enabled = dueList.isNotEmpty(), onClick = { /* future: skip a day */ }) { Text("Skip Today") }
                    }
                }
            }

            Spacer(Modifier.height(Dimensions.spaceLarge))

            // Mistakes list
            Text(text = stringResource(id = R.string.mistakes), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(mistakes) { key ->
                    ReviewItem(key = key, onRemove = null)
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(key: String, onRemove: (() -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
    ) {
        Column(modifier = Modifier.padding(Dimensions.paddingMedium)) {
            // key format: "level|question" — show nicely
            val parts = key.split("|", limit = 2)
            val levelLabel = if (parts.size == 2) "Level ${parts[0]}" else "Saved Item"
            val questionText = if (parts.size == 2) parts[1] else key

            Text(levelLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(questionText, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(Dimensions.spaceSmall))
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { scope.launch { ProgressDataStore.recordReviewResult(context, key, true) } }) { Text("Correct") }
                OutlinedButton(onClick = { scope.launch { ProgressDataStore.recordReviewResult(context, key, false) } }) { Text("Again") }
                if (onRemove != null) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.BookmarkRemove, contentDescription = stringResource(id = R.string.bookmarks))
                    }
                }
            }
        }
    }
}


