package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.localization.StringsDefinition
import com.example.localization.getStrings
import com.example.model.AppLanguage
import com.example.ui.theme.MonitorSurface
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.MonitorTextPrimary
import com.example.ui.theme.MonitorTextSecondary
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen

@Composable
fun AboutPrivacyDialog(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onDismiss: () -> Unit
) {
    // 0: Version & Info, 1: Contact, 2: Privacy Policy, 3: License & Anti-Piracy
    var selectedTab by remember { mutableIntStateOf(0) }
    val strings = getStrings(language)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MonitorSurface)
                .border(1.dp, SignalCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
            ) {
                // Header with App Title, Language Toggle and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SignalCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = SignalCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.aboutTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.aboutSubtitle,
                                color = SignalCyan,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MonitorSurfaceElevated)
                                .border(1.dp, SignalCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable(onClick = onToggleLanguage)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = SignalCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.BENGALI) "বাংলা" else "EN",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs (Version | Contact | Privacy | License)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MonitorSurfaceElevated)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    DialogTabItem(
                        title = strings.tabVersion,
                        icon = Icons.Default.Info,
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(0.95f)
                    )
                    DialogTabItem(
                        title = strings.tabContact,
                        icon = Icons.Default.ContactMail,
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1.05f)
                    )
                    DialogTabItem(
                        title = strings.tabPrivacy,
                        icon = Icons.Default.Lock,
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(0.95f)
                    )
                    DialogTabItem(
                        title = strings.tabLicense,
                        icon = Icons.Default.Shield,
                        isSelected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> VersionInfoSection(strings)
                        1 -> ContactInfoSection(strings)
                        2 -> PrivacyPolicySection(strings)
                        3 -> AntiPiracyLicenseSection(strings)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.done,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SignalCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else MonitorTextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                color = if (isSelected) Color.Black else MonitorTextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VersionInfoSection(strings: StringsDefinition) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoTile(
            label = strings.appNameLabel,
            value = "Easy USB Camera"
        )
        InfoTile(
            label = strings.developerLabel,
            value = "Ismail Nurul Islam (Bangladesh)"
        )
        InfoTile(
            label = strings.releaseVersionLabel,
            value = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        )
        InfoTile(
            label = "প্যাকেজ আইডি (Package ID)",
            value = BuildConfig.APPLICATION_ID
        )
        InfoTile(
            label = strings.engineLabel,
            value = "libusb / libuvc Native UVCCamera Engine"
        )
        InfoTile(
            label = strings.resolutionLabel,
            value = "4K Ultra HD (3840x2160), 1080p FHD, 720p HD, 576p, 480p @ 30/60 FPS"
        )
        InfoTile(
            label = strings.packageLabel,
            value = strings.packageValue
        )
        InfoTile(
            label = strings.audioEngineLabel,
            value = "PCM 48kHz / 16-Bit Stereo AudioTrack"
        )
        InfoTile(
            label = strings.purposeLabel,
            value = strings.purposeDesc
        )
    }
}

@Composable
private fun ContactInfoSection(strings: StringsDefinition) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ContactCardTile(
            label = strings.developerLabel,
            value = "Ismail Nurul Islam",
            icon = Icons.Default.Person,
            accentColor = SignalCyan,
            actionHint = null,
            onClick = null
        )

        ContactCardTile(
            label = strings.websiteLabel,
            value = "www.ismailhdmedia.blogspot.com",
            icon = Icons.Default.Public,
            accentColor = SignalCyan,
            actionHint = strings.tapToOpen,
            modifier = Modifier.testTag("contact_website_card"),
            onClick = {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.ismailhdmedia.blogspot.com")
                    )
                    context.startActivity(intent)
                } catch (_: Throwable) {
                    Toast.makeText(context, "www.ismailhdmedia.blogspot.com", Toast.LENGTH_SHORT).show()
                }
            }
        )

        ContactCardTile(
            label = strings.emailLabel,
            value = "ranahossain82@gmail.com",
            icon = Icons.Default.Email,
            accentColor = SignalAmber,
            actionHint = strings.tapToOpen,
            modifier = Modifier.testTag("contact_email_card"),
            onClick = {
                try {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:ranahossain82@gmail.com")
                    ).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Easy USB Camera Support")
                    }
                    context.startActivity(intent)
                } catch (_: Throwable) {
                    Toast.makeText(context, "ranahossain82@gmail.com", Toast.LENGTH_SHORT).show()
                }
            }
        )

        ContactCardTile(
            label = strings.locationLabel,
            value = "Bangladesh",
            icon = Icons.Default.LocationOn,
            accentColor = SignalGreen,
            actionHint = null,
            onClick = null
        )

        HighlightCard(
            title = strings.contactNoteTitle,
            body = strings.contactNoteBody,
            icon = Icons.Default.ContactMail,
            color = SignalCyan
        )
    }
}

@Composable
private fun ContactCardTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    actionHint: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MonitorSurfaceElevated),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MonitorTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = MonitorTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (actionHint != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = actionHint,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = actionHint,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicySection(strings: StringsDefinition) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HighlightCard(
            title = strings.privacy1Title,
            body = strings.privacy1Body,
            icon = Icons.Default.CheckCircle,
            color = SignalGreen
        )

        HighlightCard(
            title = strings.privacy2Title,
            body = strings.privacy2Body,
            icon = Icons.Default.VerifiedUser,
            color = SignalCyan
        )

        HighlightCard(
            title = strings.privacy3Title,
            body = strings.privacy3Body,
            icon = Icons.Default.Security,
            color = Color.White
        )
    }
}

@Composable
private fun AntiPiracyLicenseSection(strings: StringsDefinition) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HighlightCard(
            title = strings.piracy1Title,
            body = strings.piracy1Body,
            icon = Icons.Default.Shield,
            color = SignalGreen
        )

        HighlightCard(
            title = strings.piracy2Title,
            body = strings.piracy2Body,
            icon = Icons.Default.Security,
            color = SignalAmber
        )

        HighlightCard(
            title = strings.piracy3Title,
            body = strings.piracy3Body,
            icon = Icons.Default.Info,
            color = Color.LightGray
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MonitorSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                color = MonitorTextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = MonitorTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HighlightCard(
    title: String,
    body: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MonitorSurfaceElevated),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    color = MonitorTextPrimary.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
