package com.callchooser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Premium Dialog - показується коли trial expired або клік на заблокований месенджер
 */
@Composable
fun PremiumDialog(
    trialStrings: TrialStrings,
    theme: ThemeColors,
    onBuyPremium: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = theme.background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Іконка
                Text(
                    text = "⭐",
                    fontSize = 48.sp
                )
                
                // Заголовок
                Text(
                    text = trialStrings.premiumTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )
                
                // Divider
                Divider(
                    color = theme.textSecondary.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                // Features list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    trialStrings.premiumFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feature,
                                fontSize = 16.sp,
                                color = theme.textPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Ціна
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = trialStrings.premiumPrice,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = trialStrings.premiumFeatures[3], // "Одноразова оплата"
                            fontSize = 14.sp,
                            color = theme.textSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Кнопка купити
                Button(
                    onClick = onBuyPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = trialStrings.buyNow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Кнопка restore
                TextButton(
                    onClick = onRestorePurchases,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = trialStrings.restorePurchases,
                        fontSize = 14.sp,
                        color = theme.textSecondary
                    )
                }
                
                // Кнопка maybe later
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = trialStrings.maybeLater,
                        fontSize = 14.sp,
                        color = theme.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Processing Dialog - показується під час обробки покупки
 */
@Composable
fun ProcessingDialog(
    message: String,
    theme: ThemeColors
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = theme.background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = theme.textPrimary
                )
                
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
