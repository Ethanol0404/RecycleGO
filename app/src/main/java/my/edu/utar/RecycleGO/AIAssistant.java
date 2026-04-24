package my.edu.utar.RecycleGO;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIAssistant extends BottomSheetDialogFragment {

    private static final String TAG = "AIAssistant";
    private RecyclerView recyclerView;
    private NestedScrollView scrollView;
    private ChatAdapter adapter;
    private List<ChatMessage> chatMessages;
    private EditText editMessage;
    private ImageButton btnSend, btnMic, btnPlus;
    private TextView welcomeText;

    // Gemini AI Setup
    private GenerativeModelFutures model;
    private final String API_KEY = BuildConfig.GEMINI_API_KEY;

    // Camera Launcher
    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    handleImageSelected(imageBitmap);
                }
            }
    );

    // Gallery Launcher
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
    );

    // Voice Launcher
    private final ActivityResultLauncher<Intent> voiceInputLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> resultList = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (resultList != null && !resultList.isEmpty()) {
                        editMessage.setText(resultList.get(0));
                    }
                }
            }
    );

    // Permission Launchers
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) launchCamera();
                else Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
    );

    private final ActivityResultLauncher<String> recordAudioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) startVoiceInput();
                else Toast.makeText(getContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
    );

    public AIAssistant() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Correctly initialize the class-level model field
            GenerativeModel gm = new GenerativeModel("gemini-2.5-flash-lite", API_KEY);
            this.model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e(TAG, "Gemini initialization failed", e);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_chat);
        scrollView = view.findViewById(R.id.scroll_view_content);
        editMessage = view.findViewById(R.id.edit_chat_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnMic = view.findViewById(R.id.btn_mic);
        btnPlus = view.findViewById(R.id.btn_upload);
        welcomeText = view.findViewById(R.id.welcome_text);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("loggedInUsername", "User");
        if (welcomeText != null) {
            welcomeText.setText("Let’s Recycle,\n" + username + "!");
        }

        chatMessages = new ArrayList<>();
        adapter = new ChatAdapter(chatMessages);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (chatMessages.isEmpty()) {
            addMessage("♻️ Hi! I'm RecycleGO AI.\n\nYou can chat with me using:\n💬 Text\n📷 Photos\n🎤 Voice\n\nAsk me anything about recycling or waste materials!", false);
        }

        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        editMessage.setFocusableInTouchMode(true);
        editMessage.setOnClickListener(v -> {
            editMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        btnSend.setOnClickListener(v -> {
            String message = editMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, true);
                editMessage.setText("");
                generateGeminiResponse(message);
            }
        });

        btnPlus.setOnClickListener(v -> showImageSourceOptions());
        btnMic.setOnClickListener(v -> checkPermissionAndStartVoiceInput());

        updateSendButtonState();
    }

    private void generateGeminiResponse(String userMessage) {
        if (model == null) {
            addMessage("AI not initialized. Please try again later.", false);
            return;
        }

        Content content = new Content.Builder()
                .addText(
                        "You are RecycleGO AI, a recycling assistant.\n\n" +
                                "Task:\n" +
                                "1. Identify if the item is recyclable\n" +
                                "2. Explain how to recycle it briefly\n" +
                                "3. If the question is NOT about recycling, politely ask the user to ask recycling-related questions only\n\n" +
                                "User input: " + userMessage
                )
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor executor = Executors.newSingleThreadExecutor();

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> addMessage(resultText, false));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini API Failure", t);

                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {

                    String msg = (t.getMessage() != null) ? t.getMessage() : "";

                    if (msg.contains("503") ||
                            msg.contains("UNAVAILABLE") ||
                            msg.contains("Service Unavailable")) {
                        addMessage("⚠️ AI is busy right now. Please try again in a few seconds.", false);
                    }

                    else if (msg.contains("400") || msg.contains("INVALID_ARGUMENT")) {
                        addMessage("❌ Invalid request. Please try rephrasing your input.", false);
                    }

                    else if (msg.contains("401") || msg.contains("403") || msg.contains("PERMISSION_DENIED")) {
                        addMessage("🚫 API key not allowed or invalid. Check your setup.", false);
                    }

                    else if (msg.contains("404") || msg.contains("NOT_FOUND")) {
                        addMessage("❌ Model not found. Check model name.", false);
                    }

                    else if (msg.toLowerCase().contains("timeout") || msg.contains("SocketTimeoutException")) {
                        addMessage("⏳ Request timed out. Please check your internet and try again.", false);
                    }

                    else if (msg.toLowerCase().contains("unable to resolve host") ||
                            msg.toLowerCase().contains("no address associated")) {
                        addMessage("📡 No internet connection. Please check your network.", false);
                    } else if (msg.contains("429") || msg.contains("quota")) {
                            // Show a "User-Friendly" Toast message
                            addMessage ("⏳ AI is taking a quick breath. Please try again in a few seconds!", false);
                        }


                    else {
                        addMessage("❌ Unexpected error: Please try later.", false);
                    }

                });
            }
        }, executor);
    }

    private void analyzeImageWithGemini(Bitmap bitmap) {
        if (model == null) {
            addMessage("AI not initialized. Please try again later.", false);
            return;
        }

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText("Identify this material. Is it recyclable? How to recycle it? Short answer.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor executor = Executors.newSingleThreadExecutor();

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                if (getActivity() != null) {
                    String formatted = formatMessage(resultText);
                    getActivity().runOnUiThread(() -> addMessage(formatted, false));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Image analysis failed", t);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {

                        String msg = (t.getMessage() != null) ? t.getMessage() : "";

                        if (msg.contains("503") ||
                                msg.contains("UNAVAILABLE") ||
                                msg.contains("Service Unavailable")) {
                            addMessage("⚠️ AI is busy right now. Please try again in a few seconds.", false);
                        }

                        else if (msg.contains("400") || msg.contains("INVALID_ARGUMENT")) {
                            addMessage("❌ Invalid request. Please try rephrasing your input.", false);
                        }

                        else if (msg.contains("401") || msg.contains("403") || msg.contains("PERMISSION_DENIED")) {
                            addMessage("🚫 API key not allowed or invalid. Check your setup.", false);
                        }

                        else if (msg.contains("404") || msg.contains("NOT_FOUND")) {
                            addMessage("❌ Model not found. Check model name.", false);
                        }

                        else if (msg.toLowerCase().contains("timeout") || msg.contains("SocketTimeoutException")) {
                            addMessage("⏳ Request timed out. Please check your internet and try again.", false);
                        }

                        else if (msg.toLowerCase().contains("unable to resolve host") ||
                                msg.toLowerCase().contains("no address associated")) {
                            addMessage("📡 No internet connection. Please check your network.", false);
                        } else if (msg.contains("429") || msg.contains("quota")) {
                            // Show a "User-Friendly" Toast message
                            addMessage ("⏳ AI is taking a quick breath. Please try again in a few seconds!", false);
                        }


                        else {
                            addMessage("❌ Unexpected error: Please try later.", false);
                        }

                    });
                }


            }
        }, executor);
    }

    private String formatMessage(String text) {
        return text.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void updateSendButtonState() {
        boolean hasText = !editMessage.getText().toString().trim().isEmpty();
        btnSend.setEnabled(hasText);
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, action) -> {
            if (action == 0) checkPermissionAndLaunchCamera();
            else pickImageLauncher.launch("image/*");
        });
        builder.show();
    }

    private void checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(takePictureIntent);
    }

    private void checkPermissionAndStartVoiceInput() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            voiceInputLauncher.launch(intent);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getContext(), "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleImageSelected(Bitmap bitmap) {
        addMessageWithImage(bitmap);
        analyzeImageWithGemini(bitmap);
    }

    private void handleImageSelected(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            addMessageWithUri(uri);
            analyzeImageWithGemini(bitmap);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        updateList();
    }

    private void addMessageWithImage(Bitmap bitmap) {
        chatMessages.add(new ChatMessage("", true, bitmap));
        updateList();
    }

    private void addMessageWithUri(Uri uri) {
        chatMessages.add(new ChatMessage("", true, uri));
        updateList();
    }

    private void updateList() {
        adapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        recyclerView.post(() -> {
            if (scrollView != null) {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
