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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
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
import coil.compose.AsyncImage
import com.example.model.BlendModePreset
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.ImageCropRatio
import com.example.model.ImageEditorTool
import com.example.model.ImageShapeLayer
import com.example.model.NormalizedPoint
import com.example.model.PaintBrushType
import com.example.model.PaintShapeType
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val PAINT_PALETTE = listOf(
    0xFF000000L, 0xFFFFFFFFL, 0xFFFF6B00L, 0xFFFFC107L,
    0xFFE53935L, 0xFF10B981L, 0xFF2563EBL, 0xFF8B5CF6L,
    0xFFEC4899L, 0xFF06B6D4L, 0xFF78350FL, 0xFF6B7280L
)

@Composable
fun ImageEditorTabContent(
    viewModel: ImageEditorViewModel,
    onOpenProject: (Long) -> Unit,
    onNotify: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewCanvasDialog by remember { mutableStateOf(false) }

    val basePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImageUri(it) }
    }

    val overlayPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addOverlayImage(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        // Top MS Paint + Komik Narasi Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { showNewCanvasDialog = true },
                    shape = RoundedCornerShape(9.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("img_new_canvas_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.NoteAdd, null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Text("Kanvas", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                    }
                }

                Surface(
                    onClick = {
                        basePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(9.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, EnergyOrange),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("img_pick_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = EnergyOrange, modifier = Modifier.size(15.dp))
                        Text("Foto", style = MaterialTheme.typography.labelMedium, color = EnergyOrange)
                    }
                }

                IconButton(
                    onClick = viewModel::undo,
                    enabled = state.canUndo,
                    modifier = Modifier.size(36.dp)
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
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo) PureWhite else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val next = if (state.canvasZoom >= 2.2f) 1f else (state.canvasZoom + 0.5f)
                        viewModel.setCanvasZoom(next)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = "Zoom Kanvas",
                        tint = if (state.canvasZoom > 1.05f) EnergyOrange else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    onClick = { viewModel.saveEditedImageToGallery(onNotify) },
                    shape = RoundedCornerShape(9.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("img_save_gallery_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.SaveAlt, null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                        Text("Simpan", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(KomikNarasiHeroGradient)
                        .clickable {
                            viewModel.sendToVideoTimeline(
                                onProjectCreated = onOpenProject,
                                onError = onNotify
                            )
                        }
                        .padding(horizontal = 10.dp)
                        .testTag("img_send_to_timeline_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.MovieCreation, null, tint = PureWhite, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Timeline",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Interactive MS Paint + Komik Narasi Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = state.activePreviewBitmap
            if (bmp != null) {
                val doc = state.document
                val imgAspect = remember(bmp) {
                    (bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()).coerceIn(0.42f, 1.9f)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(imgAspect, matchHeightConstraintsFirst = imgAspect < 1f)
                        .graphicsLayer {
                            scaleX = state.canvasZoom
                            scaleY = state.canvasZoom
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (doc.removeBackground && doc.customBgColorHex != 0x00000000L) {
                                Color(doc.customBgColorHex)
                            } else StudioCard
                        )
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .pointerInput(state.activeTool) {
                            detectTapGestures {
                                viewModel.selectText(null)
                                viewModel.selectSticker(null)
                                viewModel.selectOverlayImage(null)
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
                        contentDescription = "Kanvas Paint Komik Narasi",
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = Modifier.fillMaxSize()
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

                    // Timpa Gambar (Overlay Images with Direct Finger Drag / Pinch / Rotate)
                    doc.overlayImages.forEach { overlay ->
                        val isSel = overlay.id == state.selectedOverlayId
                        val overlayWidth = maxWidth * overlay.scale.coerceIn(0.14f, 1.5f)
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = ((overlay.xFraction * stageW) - (stageW * overlay.scale * 0.5f)).roundToInt(),
                                        y = ((overlay.yFraction * stageH) - (stageW * overlay.scale * 0.5f)).roundToInt()
                                    )
                                }
                                .width(overlayWidth)
                                .graphicsLayer {
                                    rotationZ = overlay.rotationDeg
                                    scaleX = if (overlay.flipHorizontal) -1f else 1f
                                }
                                .alpha(overlay.opacity)
                                .then(
                                    if (isSel) Modifier.border(2.dp, EnergyOrange, RoundedCornerShape(8.dp))
                                    else Modifier.border(1.dp, PureWhite.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                )
                                .pointerInput(overlay.id) {
                                    detectTapGestures { viewModel.selectOverlayImage(overlay.id) }
                                }
                                .pointerInput(overlay.id, stageW, stageH) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        viewModel.transformOverlayImage(
                                            id = overlay.id,
                                            dx = pan.x / stageW,
                                            dy = pan.y / stageH,
                                            scaleFactor = zoom,
                                            rotDelta = rotation
                                        )
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = Uri.parse(overlay.uriString),
                                contentDescription = overlay.displayName,
                                contentScale = ContentScale.FillWidth,
                                colorFilter = when (overlay.blendMode) {
                                    BlendModePreset.SCREEN -> ColorFilter.tint(Color.White.copy(alpha = 0.2f), BlendMode.Screen)
                                    BlendModePreset.MULTIPLY -> ColorFilter.tint(Color.Gray.copy(alpha = 0.3f), BlendMode.Multiply)
                                    BlendModePreset.OVERLAY -> ColorFilter.tint(EnergyOrange.copy(alpha = 0.16f), BlendMode.Overlay)
                                    BlendModePreset.LIGHTEN -> ColorFilter.tint(Color.White.copy(alpha = 0.22f), BlendMode.Lighten)
                                    BlendModePreset.DARKEN -> ColorFilter.tint(Color.Black.copy(alpha = 0.22f), BlendMode.Darken)
                                    BlendModePreset.NORMAL -> null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Combined MS Paint Brush + Shapes + Flood Fill + Eyedropper Canvas
                    val paintInputModifier = when (state.activeTool) {
                        ImageEditorTool.PAINT -> when (state.paintBrushType) {
                            PaintBrushType.BUCKET_FILL -> Modifier.pointerInput(stageW, stageH, state.brushColorHex) {
                                detectTapGestures { offset ->
                                    viewModel.performFloodFillAt(
                                        (offset.x / stageW).coerceIn(0f, 1f),
                                        (offset.y / stageH).coerceIn(0f, 1f)
                                    )
                                }
                            }
                            PaintBrushType.EYEDROPPER -> Modifier.pointerInput(stageW, stageH) {
                                detectTapGestures { offset ->
                                    viewModel.pickColorAt(
                                        (offset.x / stageW).coerceIn(0f, 1f),
                                        (offset.y / stageH).coerceIn(0f, 1f),
                                        onPicked = onNotify
                                    )
                                }
                            }
                            else -> Modifier.pointerInput(stageW, stageH) {
                                detectDragGestures(
                                    onDragStart = { pos ->
                                        viewModel.startStroke(
                                            NormalizedPoint((pos.x / stageW).coerceIn(0f, 1f), (pos.y / stageH).coerceIn(0f, 1f))
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        viewModel.appendStrokePoint(
                                            NormalizedPoint((change.position.x / stageW).coerceIn(0f, 1f), (change.position.y / stageH).coerceIn(0f, 1f))
                                        )
                                    },
                                    onDragEnd = { viewModel.finishStroke() },
                                    onDragCancel = { viewModel.finishStroke() }
                                )
                            }
                        }
                        ImageEditorTool.SHAPES -> Modifier.pointerInput(stageW, stageH, state.activeShapeType, state.shapeFilled, state.brushColorHex, state.brushSizeDp, state.brushOpacity) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    viewModel.startShapeDrag(
                                        NormalizedPoint((pos.x / stageW).coerceIn(0f, 1f), (pos.y / stageH).coerceIn(0f, 1f))
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    viewModel.updateShapeDrag(
                                        NormalizedPoint((change.position.x / stageW).coerceIn(0f, 1f), (change.position.y / stageH).coerceIn(0f, 1f))
                                    )
                                },
                                onDragEnd = { viewModel.finishShapeDrag() },
                                onDragCancel = { viewModel.finishShapeDrag() }
                            )
                        }
                        else -> Modifier
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(paintInputModifier)
                    ) {
                        val eraserBg = Color(doc.canvasBaseColorHex)
                        fun drawStroke(
                            pts: List<NormalizedPoint>,
                            strokeId: String,
                            type: PaintBrushType,
                            color: Color,
                            widthDp: Float,
                            alpha: Float,
                            isEraser: Boolean
                        ) {
                            if (pts.size < 2) return
                            if (type == PaintBrushType.AIRBRUSH && !isEraser) {
                                val rng = Random(strokeId.hashCode())
                                val r = widthDp.dp.toPx() * 1.4f
                                val dotR = 1.4.dp.toPx()
                                pts.forEach { pt ->
                                    val cx = pt.x * size.width
                                    val cy = pt.y * size.height
                                    repeat(14) {
                                        val ang = rng.nextFloat() * 6.28318f
                                        val dist = rng.nextFloat() * r
                                        drawCircle(
                                            color = color.copy(alpha = (alpha * 0.7f).coerceIn(0.1f, 0.8f)),
                                            radius = dotR,
                                            center = Offset(cx + cos(ang) * dist, cy + sin(ang) * dist)
                                        )
                                    }
                                }
                                return
                            }
                            val path = Path()
                            pts.forEachIndexed { idx, pt ->
                                val x = pt.x * size.width
                                val y = pt.y * size.height
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            val effectiveColor = when {
                                isEraser || type == PaintBrushType.ERASER -> eraserBg
                                type == PaintBrushType.MARKER -> color.copy(alpha = (alpha * 0.45f).coerceIn(0.12f, 0.7f))
                                else -> color.copy(alpha = alpha)
                            }
                            val widthFactor = when (type) {
                                PaintBrushType.PENCIL -> 0.42f
                                PaintBrushType.MARKER -> 1.7f
                                else -> 1f
                            }
                            drawPath(
                                path = path,
                                color = effectiveColor,
                                style = Stroke(
                                    width = (widthDp * widthFactor).dp.toPx().coerceAtLeast(1.5f),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        doc.strokes.forEach { st ->
                            drawStroke(
                                pts = st.points,
                                strokeId = st.id,
                                type = st.brushType,
                                color = Color(st.colorHex),
                                widthDp = st.strokeWidthDp,
                                alpha = st.alpha,
                                isEraser = st.isEraser
                            )
                        }
                        if (state.activeStrokePoints.size >= 2) {
                            drawStroke(
                                pts = state.activeStrokePoints,
                                strokeId = "live",
                                type = state.paintBrushType,
                                color = Color(state.brushColorHex),
                                widthDp = state.brushSizeDp,
                                alpha = state.brushOpacity,
                                isEraser = state.paintBrushType == PaintBrushType.ERASER
                            )
                        }

                        doc.shapes.forEach { shape -> drawComposeShape(shape) }
                        state.draftShape?.let { drawComposeShape(it) }
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

                    // Interactive Freeform MS Paint Crop Box Overlay when CROP_TRANSFORM is active
                    if (state.activeTool == ImageEditorTool.CROP_TRANSFORM) {
                        val crop = state.draftCropRect
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cl = crop.left * size.width
                            val ct = crop.top * size.height
                            val cr = crop.right * size.width
                            val cb = crop.bottom * size.height
                            val dimColor = Color.Black.copy(alpha = 0.52f)
                            drawRect(dimColor, Offset.Zero, Size(size.width, ct))
                            drawRect(dimColor, Offset(0f, cb), Size(size.width, size.height - cb))
                            drawRect(dimColor, Offset(0f, ct), Size(cl, cb - ct))
                            drawRect(dimColor, Offset(cr, ct), Size(size.width - cr, cb - ct))
                            drawRect(
                                color = EnergyOrange,
                                topLeft = Offset(cl, ct),
                                size = Size(cr - cl, cb - ct),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        // Top-Left & Bottom-Right draggable crop handles
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((crop.left * stageW - 14f).roundToInt(), (crop.top * stageH - 14f).roundToInt()) }
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(EnergyOrange)
                                .border(2.dp, PureWhite, CircleShape)
                                .pointerInput(stageW, stageH) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        val c = state.draftCropRect
                                        viewModel.updateDraftCropRect(
                                            c.copy(
                                                left = c.left + drag.x / stageW,
                                                top = c.top + drag.y / stageH
                                            )
                                        )
                                    }
                                }
                        )
                        Box(
                            modifier = Modifier
                                .offset { IntOffset((crop.right * stageW - 14f).roundToInt(), (crop.bottom * stageH - 14f).roundToInt()) }
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                                .border(2.dp, PureWhite, CircleShape)
                                .pointerInput(stageW, stageH) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        val c = state.draftCropRect
                                        viewModel.updateDraftCropRect(
                                            c.copy(
                                                right = c.right + drag.x / stageW,
                                                bottom = c.bottom + drag.y / stageH
                                            )
                                        )
                                    }
                                }
                        )
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
            ImageToolInspector(
                state = state,
                viewModel = viewModel,
                onPickOverlayImage = {
                    overlayPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        // Bottom Tool Strip (MS Paint + Komik Narasi Tools)
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
                    ImageEditorTool.PAINT -> Icons.Filled.Brush
                    ImageEditorTool.SHAPES -> Icons.Filled.Category
                    ImageEditorTool.OVERLAY_IMAGE -> Icons.Filled.Layers
                    ImageEditorTool.CROP_TRANSFORM -> Icons.Filled.CropRotate
                    ImageEditorTool.TEXT -> Icons.Filled.TextFields
                    ImageEditorTool.STICKER -> Icons.Filled.EmojiEmotions
                    ImageEditorTool.FILTER -> Icons.Filled.FilterVintage
                    ImageEditorTool.ADJUST -> Icons.Filled.Tune
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

    if (showNewCanvasDialog) {
        AlertDialog(
            onDismissRequest = { showNewCanvasDialog = false },
            containerColor = StudioCard,
            title = { Text("Buat Kanvas Paint Baru", color = PureWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih ukuran & warna dasar kanvas kosong ala Paint:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            viewModel.createBlankCanvas(1080, 1080, 0xFFFFFFFFL)
                            showNewCanvasDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kanvas Putih 1:1 (Standar Paint)") }
                    Button(
                        onClick = {
                            viewModel.createBlankCanvas(900, 1200, 0xFFFFFFFFL)
                            showNewCanvasDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Panel Komik Putih 3:4") }
                    Button(
                        onClick = {
                            viewModel.createBlankCanvas(1280, 720, 0xFFFFFFFFL)
                            showNewCanvasDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF1A1200)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kanvas Lebar 16:9 (YouTube)") }
                    OutlinedButton(
                        onClick = {
                            viewModel.createBlankCanvas(1080, 1080, 0xFF0B0B0DL)
                            showNewCanvasDialog = false
                        },
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kanvas Hitam Pekat 1:1", color = PureWhite) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewCanvasDialog = false }) {
                    Text("Tutup", color = TextSecondary)
                }
            }
        )
    }
}

private fun DrawScope.drawComposeShape(shape: ImageShapeLayer) {
    val sx = shape.startX * size.width
    val sy = shape.startY * size.height
    val ex = shape.endX * size.width
    val ey = shape.endY * size.height
    val left = min(sx, ex)
    val top = min(sy, ey)
    val right = max(sx, ex)
    val bottom = max(sy, ey)
    val w = (right - left).coerceAtLeast(2f)
    val h = (bottom - top).coerceAtLeast(2f)
    val c = Color(shape.colorHex).copy(alpha = shape.alpha)
    val strokeStyle = Stroke(width = shape.strokeWidthDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val drawStyle = if (shape.filled) Fill else strokeStyle

    when (shape.shapeType) {
        PaintShapeType.LINE -> drawLine(c, Offset(sx, sy), Offset(ex, ey), strokeWidth = strokeStyle.width, cap = StrokeCap.Round)
        PaintShapeType.ARROW -> {
            drawLine(c, Offset(sx, sy), Offset(ex, ey), strokeWidth = strokeStyle.width, cap = StrokeCap.Round)
            val ang = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
            val headLen = (14f + shape.strokeWidthDp * 1.4f).dp.toPx()
            val a1 = ang - Math.PI / 6.0
            val a2 = ang + Math.PI / 6.0
            drawLine(c, Offset(ex, ey), Offset(ex - (headLen * cos(a1)).toFloat(), ey - (headLen * sin(a1)).toFloat()), strokeStyle.width, StrokeCap.Round)
            drawLine(c, Offset(ex, ey), Offset(ex - (headLen * cos(a2)).toFloat(), ey - (headLen * sin(a2)).toFloat()), strokeStyle.width, StrokeCap.Round)
        }
        PaintShapeType.RECTANGLE -> drawRect(c, topLeft = Offset(left, top), size = Size(w, h), style = drawStyle)
        PaintShapeType.OVAL -> drawOval(c, topLeft = Offset(left, top), size = Size(w, h), style = drawStyle)
        PaintShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo((left + right) / 2f, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(path, c, style = drawStyle)
        }
        PaintShapeType.STAR -> {
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val outerR = max(w, h) / 2f
            val innerR = outerR * 0.42f
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val rad = Math.toRadians(-90.0 + i * 36.0)
                val px = cx + (r * cos(rad)).toFloat()
                val py = cy + (r * sin(rad)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, c, style = drawStyle)
        }
        PaintShapeType.BUBBLE -> {
            val bodyH = (h * 0.78f).coerceAtLeast(10f)
            val bodyBottom = top + bodyH
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bodyBottom,
                        radiusX = 16.dp.toPx(),
                        radiusY = 16.dp.toPx()
                    )
                )
                moveTo(left + w * 0.25f, bodyBottom)
                lineTo(left + w * 0.14f, bottom)
                lineTo(left + w * 0.42f, bodyBottom)
                close()
            }
            drawPath(path, c, style = drawStyle)
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
    viewModel: ImageEditorViewModel,
    onPickOverlayImage: () -> Unit
) {
    val doc = state.document
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(185.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state.activeTool) {
            ImageEditorTool.PAINT -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaintBrushType.entries) { type ->
                        val active = state.paintBrushType == type
                        val icon = when (type) {
                            PaintBrushType.BUCKET_FILL -> Icons.Filled.FormatColorFill
                            PaintBrushType.EYEDROPPER -> Icons.Filled.Colorize
                            else -> Icons.Filled.Brush
                        }
                        FilterChip(
                            selected = active,
                            onClick = { viewModel.selectPaintBrushType(type) },
                            leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
                            label = { Text(type.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            ),
                            modifier = Modifier.testTag("paint_tool_${type.name.lowercase()}")
                        )
                    }
                }
                PaintColorPaletteRow(
                    selectedHex = state.brushColorHex,
                    onSelectColor = viewModel::setBrushColor,
                    trailingAction = {
                        TextButton(onClick = viewModel::clearBrushStrokesAndFills) {
                            Text("Bersihkan", color = CrimsonAlert)
                        }
                    }
                )
                ImgSliderRow("Ukuran", state.brushSizeDp, 2f..34f) { viewModel.setBrushSize(it) }
                ImgSliderRow("Opasitas", state.brushOpacity, 0.15f..1f) { viewModel.setBrushOpacity(it) }
                val tipText = when (state.paintBrushType) {
                    PaintBrushType.BUCKET_FILL -> "Ketuk area warna di kanvas untuk mengisi penuh dengan Ember Cat."
                    PaintBrushType.EYEDROPPER -> "Ketuk titik mana saja di gambar untuk mengambil warnanya."
                    else -> "Coret langsung dengan jari di kanvas (Pensil, Kuas, Spidol, Semprot, Penghapus)."
                }
                Text(tipText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            ImageEditorTool.SHAPES -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaintShapeType.entries) { shape ->
                        FilterChip(
                            selected = state.activeShapeType == shape,
                            onClick = { viewModel.selectShapeType(shape) },
                            label = { Text(shape.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            ),
                            modifier = Modifier.testTag("shape_tool_${shape.name.lowercase()}")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Isi Warna Penuh", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                        Switch(
                            checked = state.shapeFilled,
                            onCheckedChange = viewModel::toggleShapeFilled,
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                        )
                    }
                    if (doc.shapes.isNotEmpty()) {
                        TextButton(onClick = viewModel::deleteLastShape) {
                            Text("Hapus Terakhir", color = CrimsonAlert)
                        }
                    }
                }
                PaintColorPaletteRow(selectedHex = state.brushColorHex, onSelectColor = viewModel::setBrushColor)
                ImgSliderRow("Tebal Garis", state.brushSizeDp, 2f..24f) { viewModel.setBrushSize(it) }
                Text("Tarik jari dari satu sudut ke sudut lain di kanvas untuk membuat bentuk.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            ImageEditorTool.OVERLAY_IMAGE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPickOverlayImage,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("img_add_overlay_button")
                    ) {
                        Icon(Icons.Filled.Layers, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Timpa Gambar dari Galeri", fontWeight = FontWeight.Bold)
                    }
                    if (state.selectedOverlay != null) {
                        IconButton(onClick = viewModel::deleteSelectedOverlayImage) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Gambar Timpa", tint = CrimsonAlert)
                        }
                    }
                }
                if (doc.overlayImages.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(doc.overlayImages) { ov ->
                            FilterChip(
                                selected = ov.id == state.selectedOverlayId,
                                onClick = { viewModel.selectOverlayImage(ov.id) },
                                label = { Text(ov.displayName, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EnergyOrangeSoft,
                                    selectedLabelColor = EnergyOrange
                                )
                            )
                        }
                    }
                }
                val selOv = state.selectedOverlay
                if (selOv != null) {
                    Text("Geser, cubit & putar gambar timpaan langsung di atas kanvas.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BlendModePreset.entries) { mode ->
                            FilterChip(
                                selected = selOv.blendMode == mode,
                                onClick = { viewModel.updateSelectedOverlayImage { it.copy(blendMode = mode) } },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                    ImgSliderRow("Opasitas", selOv.opacity, 0.1f..1f) { v ->
                        viewModel.updateSelectedOverlayImage { it.copy(opacity = v) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.updateSelectedOverlayImage { it.copy(flipHorizontal = !it.flipHorizontal) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Balik H", color = PureWhite) }
                        OutlinedButton(
                            onClick = viewModel::bringSelectedOverlayToFront,
                            modifier = Modifier.weight(1f)
                        ) { Text("Ke Depan", color = GoldAccent) }
                    }
                }
            }

            ImageEditorTool.CROP_TRANSFORM -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = viewModel::applyCropNow,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("img_apply_crop_button")
                    ) {
                        Icon(Icons.Filled.Crop, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Potong Area Crop", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = viewModel::resetCrop) {
                        Text("Reset", color = GoldAccent)
                    }
                }
                Text("Seret titik oranye & kuning di kanvas untuk memilih area crop, lalu tekan Potong.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    PaintColorPaletteRow(
                        selectedHex = sel.colorHex,
                        onSelectColor = { hex -> viewModel.updateSelectedText { it.copy(colorHex = hex) } }
                    )
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
private fun PaintColorPaletteRow(
    selectedHex: Long,
    onSelectColor: (Long) -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(PAINT_PALETTE) { hex ->
                val active = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (active) 2.5.dp else 1.dp,
                            color = if (active) EnergyOrange else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(hex) }
                )
            }
        }
        trailingAction?.invoke()
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
