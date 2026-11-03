# Google Play Feature Graphic: Specification, Blueprint & AI Prompts

The **Feature Graphic** is the primary visual banner displayed at the top of your Google Play Store listing, in search highlights, and across Android recommended carousels.

> **Ready-to-Upload Banner Generated:**  
> An exact **1024 x 500 px** banner has been generated and placed in your project at:  
> [**`docs/assets/kharcha_feature_graphic_1024x500.jpg`**](file:///Users/krish/Developer/Kharcha/docs/assets/kharcha_feature_graphic_1024x500.jpg)

---

## 1. Google Play Technical Specifications

| Requirement | Specification |
| :--- | :--- |
| **Dimensions** | Exactly **1024 x 500 pixels** (aspect ratio approx 2:1) |
| **Format** | **JPG** or **24-bit PNG (no transparency / no alpha)** |
| **Max File Size** | 15 MB |
| **Safe Zone** | Keep all critical text, logos, and focal elements within the **center 820 x 400 px** area (leaving ~100px margins on sides and ~50px on top/bottom) to prevent cutoff on smaller screens. |

---

## 2. Layout Blueprint for Kharcha

A high-converting feature graphic for a personal finance app uses a **Split Composition**:

```
+-------------------------------------------------------------------------------+
|  [SAFE MARGIN ~50px TOP]                                                      |
|                                                                               |
|   LEFT / CENTER (Value Proposition)        RIGHT (Product Mockup)             |
|   ┌──────────────────────────────────┐     ┌────────────────────────────┐     |
|   │ [APP ICON]                       │     │  Floating 3D Phone Mockup  │     |
|   │                                  │     │  showing:                  │     |
|   │ KHARCHA                          │     │  • Monthly spend chart     │     |
|   │                                  │     │  • Bank SMS card (₹)       │     |
|   │ Privacy-First Expense Tracker    │     │  • Category breakdown      │     |
|   │                                  │     └────────────────────────────┘     |
|   │ [Pills / Badges]:                │                                        |
|   │ 🔒 100% Offline • Zero Internet  │                                        |
|   │ ⚡ Auto Bank SMS Tracking        │                                        |
|   └──────────────────────────────────┘                                        |
|                                                                               |
|  [SAFE MARGIN ~50px BOTTOM]                                                   |
+-------------------------------------------------------------------------------+
```

### Color Palette Tokens
* **Background Primary:** Deep Obsidian / Slate (`#0B1315` or `#0F1E22`)
* **Background Gradient Accent:** Subtle Emerald Glow (`#00382B` to `#005C42`)
* **Primary Brand Accent (Mint / Emerald):** `#00D09C` / `#22C55E`
* **Text / Highlights:** Crisp White (`#FFFFFF`) and Slate Grey (`#94A3B8`)
* **Rupee / Warning Accent:** Warm Amber / Gold (`#F59E0B`)

---

## 3. Ready-to-Use AI Image Generation Prompts

You can generate production-ready feature graphic backgrounds and compositions using these tested prompts in Midjourney, DALL-E 3, Ideogram, or Flux:

### Option 1: Modern Dark Fintech with 3D Phone Mockup (Recommended)
> **Tool:** Midjourney v6 / Ideogram 2.0 / Flux  
> **Prompt:**  
> `A high-end Google Play Store feature graphic banner for a modern personal finance app, 1024x500 landscape format. Deep emerald green and dark slate obsidian background with a subtle ambient radial glow and minimalist geometric gradient mesh. On the right side, an angled 3D smartphone mockup displaying a sleek dark-mode Material 3 personal finance dashboard with financial charts, balance meters, and incoming rupee expense transaction notifications. On the left side, clean negative space for typography. Subtle floating 3D golden rupee symbols (₹) and a glowing holographic security lock shield in the background. Ultra-clean, modern fintech aesthetic, premium Apple and Axio design language, cinematic lighting, 8k resolution, crisp photorealistic UI details. --ar 1024:500 --v 6.0 --style raw`

---

### Option 2: Pure Focus on Privacy & Offline Vault
> **Tool:** DALL-E 3 (ChatGPT Plus)  
> **Prompt:**  
> `A premium horizontal feature graphic banner measuring 1024 by 500 pixels for a privacy-first personal finance app named 'Kharcha'. Dark slate-grey and deep forest green color scheme with ambient neon emerald lighting. On the right, a realistic tilted 3D smartphone running a dark-themed expense tracker application with a spending trend graph and bank debit alerts. In the background, floating semi-transparent biometric fingerprint rings, a secure offline vault icon, and subtle floating Indian rupee coins. The left side has a clean dark gradient for text overlay. Professional fintech product photography, no clutter, no watermarks, sleek corporate branding.`

---

### Option 3: Minimalist Gradient Mesh (Ideal for Canva / Figma Customization)
If you want to place the text and screenshot yourself inside Figma or Canva:
> **Tool:** Midjourney / Flux  
> **Prompt:**  
> `A minimalist futuristic abstract background for a finance app banner, landscape aspect ratio 2:1. Dark obsidian grey fading into deep emerald green and midnight cyan mesh gradient. Soft volumetric ambient lighting, subtle frosted glass floating prisms and minimalist glowing geometric lines. Clean empty composition with generous negative space on the left and center. Cinematic studio lighting, elegant, ultra-clean wallpaper aesthetic. --ar 1024:500 --v 6.0`

---

## 4. How to Build It in Canva / Figma (5-Minute DIY Guide)

If you prefer building or customizing the graphic yourself:

1. **Create Canvas:** Set dimensions to **1024 x 500 px**.
2. **Background:** Add a gradient from `#0B1315` (top-left) to `#00382B` (bottom-right).
3. **Left Column (Typography):**
   * **App Name:** `Kharcha` (Font: *Inter*, *Plus Jakarta Sans*, or *Outfit*, Weight: **800 Bold**, Size: **56px**, Color: `#FFFFFF`).
   * **Tagline:** `Privacy-First Expense Tracker` (Weight: **600 Semi-Bold**, Size: **24px**, Color: `#00D09C`).
   * **Pill Badges:** Create two rounded pill rectangles (`#1E293B` background, 1px border):
     * Badge 1: `🔒 100% Offline • Zero Internet` (Size: **14px**, Color: `#E2E8F0`)
     * Badge 2: `⚡ Automatic Bank SMS Capture` (Size: **14px**, Color: `#E2E8F0`)
4. **Right Column (Phone Mockup):**
   * Use a free phone mockup plugin (e.g. "Mockup" in Figma or Canva's Smartmockups).
   * Insert a high-resolution screenshot of Kharcha’s Home dashboard (showing the Monthly Spend card and recent transactions).
   * Tilt the phone slightly (e.g. -8° angle) and add a soft drop shadow (`#000000`, 30% opacity, 40px blur).
5. **Export:** Export as **PNG (24-bit)** or **JPG** with quality set to 100%.

---

## 5. Critical Google Play Rules (What NOT to Do)

Google's review team actively rejects feature graphics that violate these rules:

| Do NOT Do | Why |
| :--- | :--- |
| ❌ **Do NOT include download numbers or rankings** (e.g., *"#1 Expense Tracker"*, *"100k+ Users"*). | Violates Google Play metadata policy. |
| ❌ **Do NOT put star ratings or awards** (e.g., *"★★★★★ 4.9 Stars"*). | Strictly prohibited in store listing graphics. |
| ❌ **Do NOT place text near the edges.** | When viewed on mobile, Google crops the outer 10–15% of the banner. |
| ❌ **Do NOT make the graphic transparent.** | Feature graphics must be opaque (JPG or 24-bit PNG without alpha). |
| ❌ **Do NOT add store badges** (e.g., *"Get it on Google Play"*). | Redundant and violates Google Play graphic guidelines. |
