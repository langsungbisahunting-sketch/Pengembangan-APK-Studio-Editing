package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.ImageCropRatio
import com.example.model.ImageEditorTool
import com.example.model.NormalizedPoint
import com.example.model.TextFontStyle
import com.example.ui.common.buildComposeColorMatrix
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.EnergyOrangeSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KomikNarasiHeroGradient
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ImageEditorUiState
import com.example.viewmodel.ImageEditorViewModel
import kotlin.math.roundToInt

@Composable
fun ImageEditorTabContent(
    viewModel: ImageEditorViewModel,
    onOpenProject: (Long) -> Unit,
    onNotify: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImageUri(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        // Top Action Strip: Pick photo, Undo/Redo, Save Gallery, Send to Timeline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    border = BorderStroke(1.dp, EnergyOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("img_pick_button")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.activePreviewBitmap == null) "Pilih Foto" else "Ganti",
                        style = MaterialTheme.typography.labelLarge,
                        color = EnergyOrange
                    )
                }

                IconButton(
                    onClick = viewModel::undo,
                    enabled = state.canUndo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.canUndo) PureWhite else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = viewModel::redo,
                    enabled = state.canRedo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo) PureWhite else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { viewModel.saveEditedImageToGallery(onNotify) },
                    shape = RoundedCornerShape(10.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("img_save_gallery_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.SaveAlt, null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                        Text("Simpan", style = MaterialTheme.typography.labelLarge, color = PureWhite)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KomikNarasiHeroGradient)
                        .clickable {
                            viewModel.sendToVideoTimeline(
                                onProjectCreated = onOpenProject,
                                onError = onNotify
                            )
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("img_send_to_timeline_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.MovieCreation, null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Ke Timeline",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Interactive Touch-First Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = state.activePreviewBitmap
            if (bmp == null) {
                Surface(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(KomikNarasiHeroGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = PureWhite, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = "Studio Edit Gambar Komik Narasi",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ketuk untuk memilih panel komik atau foto. Tambah tulisan, coretan kuas, filter, stiker, atau hapus latar belakang dengan sentuhan jari.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                val doc = state.document
                val imgAspect = remember(bmp, doc.rotationDegrees, doc.cropRatio) {
                    doc.cropRatio.ratio ?: run {
                        val swap = doc.rotationDegrees % 180 != 0
                        val w = if (swap) bmp.height else bmp.width
                        val h = if (swap) bmp.width else bmp.height
                        (w.toFloat() / h.coerceAtLeast(1).toFloat()).coerceIn(0.45f, 1.85f)
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(imgAspect, matchHeightConstraintsFirst = imgAspect < 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (doc.removeBackground && doc.customBgColorHex != 0x00000000L) {
                                Color(doc.customBgColorHex)
                            } else StudioCard
                        )
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                        .pointerInput(state.activeTool) {
                            detectTapGestures {
                                viewModel.selectText(null)
                                viewModel.selectSticker(null)
                            }
                        }
                ) {
                    val stageW = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
                    val stageH = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

                    if (doc.removeBackground && doc.customBgColorHex == 0x00000000L) {
                        CheckerboardCanvas(modifier = Modifier.fillMaxSize())
                    }

                    val colorMatrix = remember(doc.filterPreset, doc.adjustments) {
                        buildComposeColorMatrix(doc.filterPreset, doc.adjustments)
                    }

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Kanvas Edit Gambar",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = doc.rotationDegrees.toFloat()
                                scaleX = if (doc.flipHorizontal) -1f else 1f
                                scaleY = if (doc.flipVertical) -1f else 1f
                            }
                    )

                    if (doc.adjustments.vignette > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = doc.adjustments.vignette.coerceIn(0f, 0.85f))
                                        )
                                    )
                                )
                        )
                    }

                    // Freehand Brush Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (state.activeTool == ImageEditorTool.BRUSH) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { pos ->
                                                viewModel.startStroke(
                                                    NormalizedPoint(
                                                        (pos.x / stageW).coerceIn(0f, 1f),
                                                        (pos.y / stageH).coerceIn(0f, 1f)
                                                    )
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                viewModel.appendStrokePoint(
                                                    NormalizedPoint(
                                                        (change.position.x / stageW).coerceIn(0f, 1f),
                                                        (change.position.y / stageH).coerceIn(0f, 1f)
                                                    )
                                                )
                                            },
                                            onDragEnd = { viewModel.finishStroke() },
                                            onDragCancel = { viewModel.finishStroke() }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        fun drawStrokePath(pts: List<NormalizedPoint>, color: Color, widthDp: Float) {
                            if (pts.size < 2) return
                            val path = Path()
                            pts.forEachIndexed { idx, pt ->
                                val x = pt.x * size.width
                                val y = pt.y * size.height
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = widthDp.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        doc.strokes.forEach { st ->
                            val c = if (st.isEraser) Color(0xFF16161A) else Color(st.colorHex)
                            drawStrokePath(st.points, c, st.strokeWidthDp)
                        }
                        if (state.activeStrokePoints.size >= 2) {
                            val c = if (state.isEraserMode) Color(0xFF16161A) else Color(state.brushColorHex)
                            drawStrokePath(state.activeStrokePoints, c, state.brushSizeDp)
                        }
                    }

                    // Draggable / Pinch-Zoom / Rotatable Stickers
                    doc.stickers.forEach { sticker ->
                        val isSel = sticker.id == state.selectedStickerId
                        Surface(
                            shape = if (sticker.isBubble) {
                                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 3.dp)
                            } else RoundedCornerShape(10.dp),
                            color = Color(sticker.bgHex),
                            border = BorderStroke(
                                width = if (isSel) 2.dp else 1.5.dp,
                                color = if (isSel) PureWhite else Color(sticker.fgHex).copy(alpha = 0.65f)
                            ),
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = ((sticker.xFraction * stageW) - 56f).roundToInt(),
                                        y = ((sticker.yFraction * stageH) - 22f).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = sticker.scale
                                    scaleY = sticker.scale
                                    rotationZ = sticker.rotationDeg
                                }
                                .pointerInput(sticker.id) {
                                    detectTapGestures { viewModel.selectSticker(sticker.id) }
                                }
                                .pointerInput(sticker.id, stageW, stageH) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        viewModel.transformSticker(
                                            id = sticker.id,
                                            dx = pan.x / stageW,
                                            dy = pan.y / stageH,
                                            scaleFactor = zoom,
                                            rotDelta = rotation
                                        )
                                    }
                                }
                        ) {
                            Text(
                                text = sticker.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(sticker.fgHex),
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // Draggable / Pinch-Zoom / Rotatable Text Layers
                    doc.textLayers.forEach { item ->
                        val isSel = item.id == state.selectedTextId
                        val fontFamily = when (item.fontStyle) {
                            TextFontStyle.MODERN -> OutfitFontFamily
                            TextFontStyle.CLEAN -> PlusJakartaFontFamily
                            TextFontStyle.MONO -> JetBrainsMonoFontFamily
                        }
                        val shape = if (item.useBubble) {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                        } else RoundedCornerShape(8.dp)

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = ((item.xFraction * stageW) - 70f).roundToInt(),
                                        y = ((item.yFraction * stageH) - 24f).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = item.scale
                                    scaleY = item.scale
                                    rotationZ = item.rotationDeg
                                }
                                .clip(shape)
                                .background(Color(item.bgColorHex))
                                .then(
                                    when {
                                        isSel -> Modifier.border(2.dp, EnergyOrange, shape)
                                        item.strokeEnabled -> Modifier.border(1.dp, PureWhite.copy(alpha = 0.45f), shape)
                                        else -> Modifier
                                    }
                                )
                                .pointerInput(item.id) {
                                    detectTapGestures { viewModel.selectText(item.id) }
                                }
                                .pointerInput(item.id, stageW, stageH) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        viewModel.transformText(
                                            id = item.id,
                                            dx = pan.x / stageW,
                                            dy = pan.y / stageH,
                                            scaleFactor = zoom,
                                            rotDelta = rotation
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item.text,
                                style = TextStyle(
                                    color = Color(item.colorHex),
                                    fontFamily = fontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = if (item.hasShadow) Shadow(Color.Black.copy(alpha = 0.85f), Offset(2f, 2f), 4f) else null
                                )
                            )
                        }
                    }
                }
            }

            if (state.isBusy) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, EnergyOrange)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = EnergyOrange, modifier = Modifier.size(20.dp))
                        Text("Memproses gambar...", color = PureWhite, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Active Tool Inspector Panel
        Surface(
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            ImageToolInspector(state = state, viewModel = viewModel)
        }

        // Bottom Image Tool Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ImageEditorTool.entries) { tool ->
                val selected = tool == state.activeTool
                val icon = when (tool) {
                    ImageEditorTool.TEXT -> Icons.Filled.TextFields
                    ImageEditorTool.CROP_TRANSFORM -> Icons.Filled.CropRotate
                    ImageEditorTool.FILTER -> Icons.Filled.FilterVintage
                    ImageEditorTool.ADJUST -> Icons.Filled.Tune
                    ImageEditorTool.BRUSH -> Icons.Filled.Brush
                    ImageEditorTool.STICKER -> Icons.Filled.EmojiEmotions
                    ImageEditorTool.BG_REMOVE -> Icons.Filled.AutoFixHigh
                }
                Surface(
                    onClick = { viewModel.selectTool(tool) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) EnergyOrangeSoft else StudioCard,
                    border = BorderStroke(1.dp, if (selected) EnergyOrange else StudioBorder),
                    modifier = Modifier.testTag("img_tool_${tool.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, null, tint = if (selected) EnergyOrange else TextSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = tool.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) EnergyOrange else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckerboardCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = 14.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        for (r in 0..rows) {
            for (c in 0..cols) {
                val color = if ((r + c) % 2 == 0) Color(0xFF232329) else Color(0xFF18181D)
                drawRect(
                    color = color,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}

@Composable
private fun ImageToolInspector(
    state: ImageEditorUiState,
    viewModel: ImageEditorViewModel
) {
    val doc = state.document
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state.activeTool) {
            ImageEditorTool.TEXT -> {
                val sel = state.selectedText
                var draft by remember(sel?.id) { mutableStateOf(sel?.text ?: "") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { v ->
                            draft = v
                            if (sel != null) viewModel.updateSelectedText { it.copy(text = v) }
                        },
                        placeholder = { Text("Tulis tulisan / dialog baru...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("img_text_input")
                    )
                    if (sel == null) {
                        Button(
                            onClick = {
                                if (draft.isNotBlank()) {
                                    viewModel.addText(draft)
                                    draft = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                            modifier = Modifier.testTag("img_text_add_button")
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    } else {
                        IconButton(onClick = viewModel::deleteSelectedText) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Tulisan", tint = CrimsonAlert)
                        }
                    }
                }
                Text("Sentuh, geser, cubit & putar tulisan langsung di atas gambar.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (sel != null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TextFontStyle.entries) { font ->
                            FilterChip(
                                selected = sel.fontStyle == font,
                                onClick = { viewModel.updateSelectedText { it.copy(fontStyle = font) } },
                                label = { Text(font.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EnergyOrangeSoft,
                                    selectedLabelColor = EnergyOrange
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = sel.useBubble,
                                onClick = { viewModel.updateSelectedText { it.copy(useBubble = !it.useBubble) } },
                                label = { Text("Balon Box") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = sel.strokeEnabled,
                                onClick = { viewModel.updateSelectedText { it.copy(strokeEnabled = !it.strokeEnabled) } },
                                label = { Text("Stroke") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = sel.hasShadow,
                                onClick = { viewModel.updateSelectedText { it.copy(hasShadow = !it.hasShadow) } },
                                label = { Text("Bayangan") }
                            )
                        }
                    }
                    val textColors = listOf(0xFFFFFFFFL, 0xFFFF6B00L, 0xFFFFC107L, 0xFF0B0B0DL, 0xFF38BDF8L)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Warna:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        textColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (sel.colorHex == hex) 2.dp else 1.dp,
                                        color = if (sel.colorHex == hex) EnergyOrange else StudioBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.updateSelectedText { it.copy(colorHex = hex) } }
                            )
                        }
                    }
                }
            }

            ImageEditorTool.CROP_TRANSFORM -> {
                Text("Rasio Pangkas", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ImageCropRatio.entries) { ratio ->
                        FilterChip(
                            selected = doc.cropRatio == ratio,
                            onClick = { viewModel.setCropRatio(ratio) },
                            label = { Text(ratio.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = viewModel::rotate90, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.RotateRight, null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Putar 90°", color = PureWhite)
                    }
                    OutlinedButton(onClick = viewModel::toggleFlipH, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Flip, null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Balik H", color = if (doc.flipHorizontal) EnergyOrange else PureWhite)
                    }
                    OutlinedButton(onClick = viewModel::toggleFlipV, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Flip, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Balik V", color = if (doc.flipVertical) GoldAccent else PureWhite)
                    }
                }
            }

            ImageEditorTool.FILTER -> {
                Text("Preset Filter Komik Narasi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterPreset.entries) { preset ->
                        FilterChip(
                            selected = doc.filterPreset == preset,
                            onClick = { viewModel.setFilter(preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            )
                        )
                    }
                }
            }

            ImageEditorTool.ADJUST -> {
                val adj = doc.adjustments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Penyesuaian Cahaya & Warna", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                    TextButton(onClick = { viewModel.updateAdjustments(ColorAdjustments()) }) {
                        Text("Reset", color = CrimsonAlert)
                    }
                }
                ImgSliderRow("Kecerahan", adj.brightness, -0.4f..0.4f) { viewModel.updateAdjustments(adj.copy(brightness = it)) }
                ImgSliderRow("Kontras", adj.contrast, -0.4f..0.4f) { viewModel.updateAdjustments(adj.copy(contrast = it)) }
                ImgSliderRow("Saturasi", adj.saturation, -0.8f..0.8f) { viewModel.updateAdjustments(adj.copy(saturation = it)) }
                ImgSliderRow("Exposure", adj.exposure, -0.4f..0.4f) { viewModel.updateAdjustments(adj.copy(exposure = it)) }
                ImgSliderRow("Ketajaman", adj.sharpen, 0f..1f) { viewModel.updateAdjustments(adj.copy(sharpen = it)) }
                ImgSliderRow("Vignette", adj.vignette, 0f..0.85f) { viewModel.updateAdjustments(adj.copy(vignette = it)) }
            }

            ImageEditorTool.BRUSH -> {
                val brushColors = listOf(0xFFFF6B00L, 0xFFFFC107L, 0xFFFFFFFFL, 0xFFE53935L, 0xFF10B981L, 0xFF0B0B0DL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        brushColors.forEach { hex ->
                            val active = !state.isEraserMode && state.brushColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(if (active) 2.5.dp else 1.dp, if (active) EnergyOrange else StudioBorder, CircleShape)
                                    .clickable { viewModel.setBrushColor(hex) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = state.isEraserMode,
                            onClick = { viewModel.toggleEraser(!state.isEraserMode) },
                            label = { Text("Penghapus") }
                        )
                        TextButton(onClick = viewModel::clearBrushStrokes) {
                            Text("Bersihkan", color = CrimsonAlert)
                        }
                    }
                }
                ImgSliderRow("Ukuran Kuas", state.brushSizeDp, 2f..26f) { viewModel.setBrushSize(it) }
                Text("Coret langsung dengan jari pada gambar di atas.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            ImageEditorTool.STICKER -> {
                val stickers = listOf(
                    "BOOM!" to (0xFFFFC107L to 0xFF1A1200L),
                    "BERSAMBUNG..." to (0xFFFF6B00L to 0xFFFFFFFFL),
                    "KRAK!!" to (0xFFE53935L to 0xFFFFFFFFL),
                    "WHOSH!" to (0xFF16161AL to 0xFFFFFFFFL),
                    "NANI?!" to (0xFFFFFFFFL to 0xFF0B0B0DL),
                    "BABAK BARU" to (0xFF10B981L to 0xFF041C11L)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ketuk untuk menambahkan stiker komik:", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                    if (state.selectedSticker != null) {
                        IconButton(onClick = viewModel::deleteSelectedSticker, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Stiker", tint = CrimsonAlert)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stickers) { (label, colors) ->
                        Surface(
                            onClick = { viewModel.addSticker(label, colors.first, colors.second) },
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 2.dp),
                            color = Color(colors.first),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(colors.second),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Text("Geser, cubit & putar stiker langsung dengan jari.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            ImageEditorTool.BG_REMOVE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hapus Latar Belakang Otomatis", style = MaterialTheme.typography.labelLarge, color = PureWhite)
                    Switch(
                        checked = doc.removeBackground,
                        onCheckedChange = viewModel::toggleBackgroundRemoval,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                    )
                }
                ImgSliderRow("Toleransi", doc.bgTolerance, 0.08f..0.60f) { viewModel.setBgTolerance(it) }
                val bgSwatches = listOf(0x00000000L, 0xFF0B0B0DL, 0xFFFFFFFFL, 0xFFFF6B00L, 0xFFFFC107L)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Latar Ganti:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    bgSwatches.forEach { hex ->
                        val active = doc.customBgColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (hex == 0x00000000L) GoldSoft else Color(hex))
                                .border(if (active) 2.dp else 1.dp, if (active) EnergyOrange else StudioBorder, CircleShape)
                                .clickable { viewModel.setCustomBgColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hex == 0x00000000L) {
                                Text("PNG", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = GoldAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImgSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
        )
    }
}
