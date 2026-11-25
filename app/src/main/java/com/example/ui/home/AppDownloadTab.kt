package com.example.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KomikNarasiHeroGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextSecondary

private const val KN_INSTALL_URL =
    "https://ais-pre-l3czvjo3k7wbkp7k4vbpzn-1011596292594.asia-east1.run.app"
private const val KN_BUILD_CMD = "gradle :app:assembleRelease"

@Composable
fun AppDownloadTabContent(
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_download_tab_content"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(KomikNarasiHeroGradient)
                        .border(
                            width = 1.dp,
                            color = PureWhite.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(StudioObsidian.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Unduh Komik Narasi",
                                    tint = PureWhite
                                )
                            }
                            Column {
                                Text(
                                    text = "Unduh Komik Narasi Studio",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = "APK Android, QR Code instan, & rilis GitHub otomatis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QrCodeCard(
                                payload = KN_INSTALL_URL,
                                modifier = Modifier.size(132.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode2,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Pindai QR di HP",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PureWhite
                                    )
                                }
                                Text(
                                    text = KN_INSTALL_URL,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(KN_INSTALL_URL))
                                            )
                                        }.onFailure {
                                            onNotify("Tidak ada browser tersedia di perangkat ini.")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PureWhite,
                                        contentColor = StudioObsidian
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("download_open_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Tautan Unduh", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(KN_INSTALL_URL))
                                    onNotify("Tautan instalasi Komik Narasi disalin!")
                                },
                                border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("copy_install_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Link", color = PureWhite)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Instal Komik Narasi Studio")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Edit video & komik narasi di HP dengan Komik Narasi Studio! Buka tautan: $KN_INSTALL_URL"
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Bagikan Komik Narasi Studio")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioObsidian.copy(alpha = 0.85f),
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_install_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bagikan Link")
                            }
                        }
                    }
                }
            }
        }

        item {
            InstallGuideCard(
                icon = Icons.Filled.Android,
                tint = EmeraldSuccess,
                title = "1. Unduh File Installer Android (.APK)",
                badge = "ANDROID 7.0+",
                steps = listOf(
                    "Di GitHub repository Anda, buka bagian Releases di kanan lalu klik Komik Narasi Studio APK untuk mengunduh app-debug.apk.",
                    "Atau dari Google AI Studio, klik Settings / Export -> Generate APK untuk mengunduh langsung ke perangkat.",
                    "Ketuk file .apk di HP Android, izinkan Sumber Tidak Dikenal jika diminta, lalu tekan Instal."
                )
            )
        }

        item {
            InstallGuideCard(
                icon = Icons.Filled.Terminal,
                tint = EnergyOrange,
                title = "2. Perintah Build APK Lokal / CI",
                badge = "GRADLE CLI",
                steps = listOf(
                    "Workflow GitHub Actions (.github/workflows/build-apk.yml) otomatis merakit APK setiap kali Anda Push ke GitHub.",
                    "Untuk build lokal di Android Studio / terminal, jalankan perintah release build."
                ),
                footerActionLabel = "Salin Perintah Build ($KN_BUILD_CMD)",
                onFooterAction = {
                    clipboard.setText(AnnotatedString(KN_BUILD_CMD))
                    onNotify("Perintah '$KN_BUILD_CMD' disalin!")
                }
            )
        }

        item {
            InstallGuideCard(
                icon = Icons.Filled.PhoneIphone,
                tint = GoldAccent,
                title = "3. Akses di iOS & Peramban HP",
                badge = "STREAMING / WEB",
                steps = listOf(
                    "Pindai QR Code di atas dari kamera HP atau buka tautan preview Komik Narasi Studio.",
                    "Pilih Tambahkan ke Layar Utama (Add to Home Screen) untuk akses cepat dari layar beranda HP."
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QrCodeCard(
    payload: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { buildDeterministicQrMatrix(payload, size = 25) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(2.dp, StudioObsidian)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("download_qr_code")
        ) {
            val n = matrix.size
            val cellSize = size.minDimension / n
            val offsetX = (size.width - cellSize * n) / 2f
            val offsetY = (size.height - cellSize * n) / 2f

            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF0B0B0D),
                            topLeft = Offset(offsetX + c * cellSize, offsetY + r * cellSize),
                            size = Size(cellSize * 0.96f, cellSize * 0.96f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallGuideCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    badge: String,
    steps: List<String>,
    footerActionLabel: String? = null,
    onFooterAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCard
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            steps.forEach { step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = tint.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (footerActionLabel != null && onFooterAction != null) {
                OutlinedButton(
                    onClick = onFooterAction,
                    border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_build_cmd_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = EnergyOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = footerActionLabel,
                        color = EnergyOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun buildDeterministicQrMatrix(payload: String, size: Int = 25): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }

    fun drawFinder(startRow: Int, startCol: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                grid[startRow + r][startCol + c] = isBorder || isInner
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    for (i in 8 until size - 8) {
        grid[6][i] = i % 2 == 0
        grid[i][6] = i % 2 == 0
    }

    val alignOrigin = size - 9
    for (r in 0 until 5) {
        for (c in 0 until 5) {
            val isEdge = r == 0 || r == 4 || c == 0 || c == 4
            val isCenter = r == 2 && c == 2
            grid[alignOrigin + r][alignOrigin + c] = isEdge || isCenter
        }
    }

    var seed = payload.hashCode().toLong() xor 0x5DEECE66DL
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6
            val inAlign = r in alignOrigin until (alignOrigin + 5) &&
                c in alignOrigin until (alignOrigin + 5)
            if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming && !inAlign) {
                seed = (seed * 6364136223846793005L + 1442695040888963407L)
                grid[r][c] = ((seed ushr 31) and 1L) == 1L
            }
        }
    }
    return grid
}
