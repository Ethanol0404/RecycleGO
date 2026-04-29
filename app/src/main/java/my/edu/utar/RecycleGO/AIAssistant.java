package my.edu.utar.RecycleGO;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.UserRecord;
import my.edu.utar.RecycleGO.utils.ApiKeyManager;

public class AIAssistant extends BottomSheetDialogFragment {

    private static final String TAG = "AIAssistant";
    private RecyclerView recyclerView;
    private NestedScrollView scrollView;
    private ChatAdapter adapter;
    private List<ChatMessage> chatMessages;
    private EditText editMessage;
    private ImageButton btnSend, btnMic, btnPlus;
    private TextView welcomeText;
    private String userProfilePicUrl = "";

    private GenerativeModelFutures model;

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

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
    );

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

    public AIAssistant() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeModel();
        loadUserProfile();
    }

    private void loadUserProfile() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("loggedInUid", "");
        if (!uid.isEmpty()) {
            new FirestoreManager().getUser(uid, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null && isAdded()) {
                        userProfilePicUrl = user.getProfilePicUrl();
                        if (user.getUsername() != null) {
                            String username = user.getUsername();
                            if (welcomeText != null) {
                                welcomeText.setText("Let’s Recycle,\n" + username + "!");
                            }
                            // Also update local preference for consistency
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("loggedInUsername", username);
                            editor.apply();
                        }
                    }
                }
                @Override
                public void onFailure(String error) {}
            });
        }
    }

    private void initializeModel() {
        try {
            String apiKey = ApiKeyManager.getCurrentKey(requireContext());
            if (apiKey.isEmpty()) {
                Log.e(TAG, "No API key found in ApiKeyManager!");
                return;
            }
            // Using stable Gemini 1.5 Flash model
            GenerativeModel gm = new GenerativeModel("gemini-2.5-flash-lite", apiKey);
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
                
                // Set the background color of the bottom sheet itself
                SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                String bgColorCode = prefs.getString("theme_color", "#D1E29B");
                bottomSheet.setBackgroundColor(Color.parseColor(bgColorCode));
            }
        });
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            addMessage("♻️ Hi! I'm RecycleGO AI.You can chat with me using:💬Text 📷Photos🎤Voice. Ask me anything about recycling or waste materials!", false);
        }

        editMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSendButtonState(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String message = editMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, true);
                editMessage.setText("");
                generateGeminiResponse(message, 0);
            }
        });

        btnPlus.setOnClickListener(v -> showImageSourceOptions());
        btnMic.setOnClickListener(v -> checkPermissionAndStartVoiceInput());
        
        applyCustomTheme(view);
        updateSendButtonState();
    }

    private void applyCustomTheme(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String bottomColorCode = prefs.getString("bottom_color", "#265200");
        int bottomColor = Color.parseColor(bottomColorCode);

        if (welcomeText != null) welcomeText.setTextColor(bottomColor);
        if (btnPlus != null) btnPlus.setColorFilter(bottomColor);
        if (btnMic != null) btnMic.setColorFilter(bottomColor);
    }

    private void generateGeminiResponse(String userMessage, int retryCount) {
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
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(result.getText(), false));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini Failure (Attempt " + retryCount + ")", t);
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (retryCount < ApiKeyManager.getApiKeysCount() - 1) {
                        ApiKeyManager.rotateKey(requireContext());
                        initializeModel();
                        generateGeminiResponse(userMessage, retryCount + 1);
                    } else {
                        String errorMsg = t.getMessage() != null ? t.getMessage() : t.toString();
                        if (errorMsg.contains("429"))
                            addMessage("⏳ AI is taking a quick breath. Please try again in a few seconds!", false);
                        else if (errorMsg.contains("401") || errorMsg.contains("403"))
                            addMessage("🚫 API key not allowed or invalid. Check your setup.", false);
                        else if (errorMsg.contains("404"))
                            addMessage("❌ Model not found. Check model name.", false);
                        else if (errorMsg.contains("400"))
                            addMessage("❌ Invalid request. Please try rephrasing your input.", false);
                        else if (errorMsg.toLowerCase().contains("timeout") || errorMsg.contains("SocketTimeoutException"))
                            addMessage("⏳ Request timed out. Please check your internet and try again.", false);
                        else if (errorMsg.contains("503") || errorMsg.contains("UNAVAILABLE") || errorMsg.contains("Service Unavailable"))
                            addMessage("⚠️ AI is busy right now. Please try again in a few seconds.", false);
                        else addMessage("❌ Unexpected error: Please try later.", false);
                    }
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void analyzeImageWithGemini(Bitmap bitmap, int retryCount) {
        if (model == null) {
            addMessage("AI not initialized. Please try again later.", false);
            return;
        }

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText("Identify this material. Is it recyclable? How to recycle it? Short answer.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(result.getText(), false));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Image analysis failed", t);
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (retryCount < ApiKeyManager.getApiKeysCount() - 1) {
                        ApiKeyManager.rotateKey(requireContext());
                        initializeModel();
                        analyzeImageWithGemini(bitmap, retryCount + 1);
                    } else {
                        if (t.getMessage().contains("429"))
                            addMessage("⏳ AI is taking a quick breath. Please try again in a few seconds!", false);
                        else if (t.getMessage().contains("401") || t.getMessage().contains("403"))
                            addMessage("🚫 API key not allowed or invalid. Check your setup.", false);
                        else if (t.getMessage().contains("404"))
                            addMessage("❌ Model not found. Check model name.", false);
                        else if (t.getMessage().contains("400"))
                            addMessage("❌ Invalid request. Please try rephrasing your input.", false);
                        else if (t.getMessage().toLowerCase().contains("timeout") || t.getMessage().contains("SocketTimeoutException"))
                            addMessage("⏳ Request timed out. Please check your internet and try again.", false);
                        else if (t.getMessage().toLowerCase().contains("unable to resolve host") || t.getMessage().toLowerCase().contains("no address associated"))
                            addMessage("📡 No internet connection. Please check your network.", false);
                        else if (t.getMessage().contains("503") || t.getMessage().contains("UNAVAILABLE") || t.getMessage().contains("Service Unavailable"))
                            addMessage("⚠️ AI is busy right now. Please try again in a few seconds.", false);
                        else addMessage("❌ Unexpected error: Please try later.", false);
                    }
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void addMessage(String message, boolean isUser) {
        ChatMessage msg = new ChatMessage(message, isUser);
        if (isUser) msg.setUserProfilePicUrl(userProfilePicUrl);
        chatMessages.add(msg);
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.post(() -> { if (scrollView != null) scrollView.fullScroll(View.FOCUS_DOWN); });
    }

    private void handleImageSelected(Bitmap bitmap) {
        ChatMessage msg = new ChatMessage("", true, bitmap);
        msg.setUserProfilePicUrl(userProfilePicUrl);
        chatMessages.add(msg);
        adapter.notifyItemInserted(chatMessages.size() - 1);
        analyzeImageWithGemini(bitmap, 0);
    }

    private void handleImageSelected(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            handleImageSelected(bitmap);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSendButtonState() {
        if (editMessage == null || btnSend == null) return;
        
        String message = editMessage.getText().toString().trim();
        boolean hasText = !message.isEmpty();
        btnSend.setEnabled(hasText);
        
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String themeColorCode = prefs.getString("bottom_color", "#265200");
        int themeColor = Color.parseColor(themeColorCode);

        if (hasText) {
            btnSend.setBackgroundTintList(ColorStateList.valueOf(themeColor));
            btnSend.setAlpha(1.0f);
        } else {
            // Light color (Gray) when empty
            btnSend.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnSend.setAlpha(0.6f);
        }
    }

    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(requireContext()).setTitle("Select Image").setItems(options, (d, i) -> {
            if (i == 0) checkPermissionAndLaunchCamera(); else pickImageLauncher.launch("image/*");
        }).show();
    }
    private void checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera();
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
    private void launchCamera() { takePictureLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); }
    private void checkPermissionAndStartVoiceInput() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceInput();
        else recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try { voiceInputLauncher.launch(intent); } catch (Exception e) { Toast.makeText(getContext(), "Voice input not supported", Toast.LENGTH_SHORT).show(); }
    }
}
