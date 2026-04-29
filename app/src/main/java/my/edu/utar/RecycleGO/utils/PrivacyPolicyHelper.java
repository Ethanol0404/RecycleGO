package my.edu.utar.RecycleGO.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

public class PrivacyPolicyHelper {
    public static void showPrivacyPolicy(Context context, CheckBox checkBox) {
        String policy = "# 关于RécycleGO与隐私的声明\n\n" +
                "**更新日期：2026年4月28日**\n\n" +
                "“RécycleGO”是由**Timothy Wong Siew Ruey** 开发的一款用于**识别可回收物品、提供附近回收点推荐及智能问答服务**的应用。我们高度重视您的个人信息和隐私保护，并将按照法律法规及行业标准保护您的信息安全。\n\n" +
                "# 导语\n\n" +
                "# 一、我们如何收集和使用您的个人信息\n\n" +
                "我们仅在合法性基础上收集和使用您的个人信息，包括但不限于以下情况：\n\n" +
                "**1. 账户信息**\n" +
                "当您注册或登录时，我们会收集您的电子邮箱及账户信息，用于身份验证及账户管理。\n\n" +
                "**2. 设备信息**\n" +
                "包括设备型号、操作系统版本，用于保障应用正常运行。\n\n" +
                "**3. 日志信息**\n" +
                "包括IP地址、使用记录、崩溃日志，用于优化系统性能。\n\n" +
                "**4. 位置信息**\n" +
                "在获得您的授权后，我们会收集位置信息，用于推荐附近回收点。\n\n" +
                "**5. 相机与图像数据**\n" +
                "在获得授权后，我们会使用相机拍摄图片，用于识别可回收物品。\n\n" +
                "**6. AI聊天数据**\n" +
                "您输入的文本或上传内容将用于生成智能回复并优化服务质量。\n\n" +
                "# 二、设备权限调用\n\n" +
                "为实现功能，我们可能会请求以下权限：\n\n" +
                "**相机权限**：用于拍摄物品进行识别\n" +
                "**位置权限**：用于推荐附近回收点\n\n" +
                "我们仅在必要时请求权限，且您可以随时在设备设置中关闭。\n\n" +
                "# 三、对未成年人的保护\n\n" +
                "本应用不面向13岁以下未成年人。我们不会主动收集未成年人的个人信息。\n\n" +
                "# 四、与第三方共享\n\n" +
                "我们不会出售您的个人信息。\n" +
                "仅在以下情况下共享：\n\n" +
                "* 向云服务或技术支持服务提供商提供必要数据\n" +
                "* 根据法律法规要求提供\n\n" +
                "# 五、第三方SDK\n\n" +
                "本应用可能接入第三方服务（如云服务、分析工具），这些服务可能会收集必要信息。具体请参考第三方隐私政策。\n\n" +
                "# 六、如何保护您的个人信息\n\n" +
                "我们采用包括HTTPS在内的安全技术措施保护您的数据安全。\n\n" +
                "# 七、管理您的个人信息\n\n" +
                "您可以：\n\n" +
                "* 访问或更正个人信息\n" +
                "* 删除账户及数据\n" +
                "* 撤回授权（如关闭定位权限）\n\n" +
                "您可通过应用内设置或联系我们实现上述权利。\n\n" +
                "# 八、信息存储地点及期限\n\n" +
                "* 数据可能存储在云服务器中\n" +
                "* 我们仅在必要期限内保存数据\n" +
                "* 超出期限将删除或匿名化处理\n\n" +
                "# 九、如何联系我们\n\n" +
                "如您有任何问题运行或请求，请通过以下方式联系我们：\n\n" +
                "* 电子邮箱：recyclegomy@gmail.com\n\n" +
                "如您对处理结果不满意，可向相关监管机构投诉或通过法律途径解决。\n\n" +
                "==============================\n\n" +
                "# Privacy Policy for RécycleGO\n\n" +
                "**Last Updated: April 28, 2026**\n\n" +
                "RécycleGO is an application developed by **Timothy Wong Siew Ruey** that provides **recyclable item identification, nearby recycling recommendations, and AI-based assistance**. We are committed to protecting your personal data in accordance with applicable laws and industry standards.\n\n" +
                "# Introduction\n\n" +
                "# 1. How We Collect and Use Your Personal Data\n\n" +
                "We collect and use personal data only when there is a legal basis, including your consent or service necessity:\n\n" +
                "**1. Account Information**\n" +
                "Email address and account details for login and account management.\n\n" +
                "**2. Device Information**\n" +
                "Device model and operating system to ensure proper functionality.\n\n" +
                "**3. Log Information**\n" +
                "IP address, usage data, and crash logs for system optimization.\n\n" +
                "**4. Location Data**\n" +
                "With your consent, used to recommend nearby recycling centers.\n\n" +
                "**5. Camera and Image Data**\n" +
                "Used to capture images for recyclable item identification.\n\n" +
                "**6. AI Chat Data**\n" +
                "User inputs are processed to generate responses and improve service quality.\n\n" +
                "# 2. Device Permissions\n\n" +
                "We may request the following permissions:\n\n" +
                "**Camera**: for item identification\n" +
                "**Location**: for nearby recommendations\n\n" +
                "Permissions are requested only when necessary and can be disabled anytime.\n\n" +
                "# 3. Protection of Minors\n\n" +
                "This app is not intended for children under 13, and we do not knowingly collect their data.\n\n" +
                "# 4. Data Sharing\n\n" +
                "We do not sell personal data.\n" +
                "We may share data only:\n\n" +
                "* With cloud or technical service providers\n" +
                "* To comply with legal obligations\n\n" +
                "# 5. Third-Party SDKs\n\n" +
                "We may use third-party services (e.g., cloud or analytics), which may collect necessary data under their own policies.\n\n" +
                "# 6. Data Security\n\n" +
                "We use security measures such as HTTPS encryption to protect your data.\n\n" +
                "# 7. Managing Your Data\n\n" +
                "You may:\n\n" +
                "* Access or correct your data\n" +
                "* Delete your account and data\n" +
                "* Withdraw consent (e.g., disable permissions)\n\n" +
                "# 8. Data Storage and Retention\n\n" +
                "* Data may be stored on cloud servers\n" +
                "* Retained only as necessary\n" +
                "* Deleted or anonymized after retention period\n\n" +
                "# 9. Contact Us\n\n" +
                "If you have questions or requests:\n\n" +
                "* Email: recyclegomy@gmail.com";

        // Convert Markdown-style formatting to HTML
        String htmlPolicy = policy
                .replaceAll("(?m)^### (.*)$", "<b>$1</b>")
                .replaceAll("(?m)^## (.*)$", "<b>$1</b>")
                .replaceAll("(?m)^# (.*)$", "<b><big>$1</big></b>")
                .replaceAll("(?m)^\\* (.*)$", "&bull; $1")
                .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                .replace("\n", "<br>");

        TextView textView = new TextView(context);
        textView.setText(Html.fromHtml(htmlPolicy, Html.FROM_HTML_MODE_LEGACY));
        textView.setPadding(48, 48, 48, 48);
        textView.setTextSize(16);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(textView);

        new AlertDialog.Builder(context)
                .setTitle("Privacy Policy / 隐私政策")
                .setView(scrollView)
                .setPositiveButton("Close 关闭", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
