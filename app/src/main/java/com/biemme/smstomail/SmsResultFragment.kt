package com.biemme.smstomail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

class SmsResultFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val smsMessage = arguments?.getString("sms_message") ?: ""
        val mailResult = arguments?.getString("mail_result") ?: ""
        return ComposeView(requireContext()).apply {
            setContent {
                SmsResultContent(smsMessage, mailResult)
            }
        }
    }
}

@Composable
fun SmsResultContent(smsMessage: String, mailResult: String) {
    Column(Modifier.padding(16.dp)) {
        Text("SMS ricevuto:")
        Text(smsMessage, modifier = Modifier.padding(bottom = 16.dp))
        Text("Risultato invio mail:")
        Text(mailResult)
    }
}

