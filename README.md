# app-sos

**この repo は動かない。** `sos.etzhayyim.com` 向けの System-of-Systems
可視化アプリとして `etzhayyim/root` から抽出された **14 ファイル・9,411 バイトの
scaffold** であり、ビルドもデプロイもできない —— 依存が解決しないので
`pnpm install` の時点で止まる。

読者が最初に知るべきことはそれで、この README はその根拠と、
**直すとしたら何から手を付けるか**を置く。

`README.edn`（機械可読な正本）は `:kind :app` / `:role :systems-intelligence-application`
とだけ言い、`AGENTS.md`・`PROJECT.jsonld`・`appview/README.md` の 3 つは
**この repo に無いもの**を現在形で記述している。だから
「ディレクトリを開いて読む」経路では、実態と逆のことを学ぶ。

計測日 2026-08-18。すべて `docs/operator-quickstart.md` の手順で再現でき、
`docs/verify-docs-claims.cljk` が機械で再検査する。

---

## 1. 何が入っているか

抽出時点の追跡ファイルは **16** —— **14 が継承**（`etzhayyim/root`）で、
**2 が抽出時の追加**（`README.edn` / `migration.edn`、`migration.edn` の
`:allowed-additions` が許した 2 つとちょうど一致する）。
いま `git ls-files` が 19 を返すのは、この README と `docs/` の 2 ファイルを
後から足したためで、**継承 14 と抽出時の追加 2 は 1 つも動いていない**
（§4 の custody 照合が毎回それを確かめる）。

実行されるコードは Svelte の入口 3 ファイルだけ:

| ファイル | 中身 |
|---|---|
| `svelte/src/App.svelte` | 22 行。`<h1>` と `<p>Vite entry scaffold after SvelteKit cleanup.</p>` |
| `svelte/src/main.ts` | `mount(App, …)` の 5 行 |
| `svelte/src/svelte.d.ts` | `*.svelte` の型宣言 |

`AGENTS.md` は App.svelte を "placeholder" と正しく呼んでいる。**そこは正確**。
問題は同じファイルが、その placeholder の周りに無いものを在るかのように書いていること。

## 2. なぜビルドできないか（依存が 2 つとも解決しない）

`svelte/package.json` は 5 dependencies + 9 devDependencies を宣言し、
そのうち **2 つが `workspace:*`** プロトコルを使う:

- `@etzhayyim/design-system`
- `@etzhayyim/vite-plugin-safe-builder`

`workspace:*` は「同じ pnpm workspace の中の兄弟パッケージを使う」という意味で、
**workspace root が無いと解決できない**。この repo に workspace root は無い
（`pnpm-workspace.yaml` も、ルートの `package.json` も、lockfile も無い）。
npm registry にも無い —— 両方とも **HTTP 404**。

### これは抽出が壊したのではない

上流の monorepo に戻しても直らない。`etzhayyim/root@afe5f1d9` の
`pnpm-workspace.yaml` は **35 個の glob** を並べているが、
**この app の svelte ディレクトリに一致するものが 1 つも無い**。
ワイルドカードは 2 つあるものの、どちらも別の場所を指す:

```
60-apps/etzhayyim-project-*/kotoba
60-apps/etzhayyim-project-*/xrpc-adapter
```

必要なのは `60-apps/etzhayyim-project-sos/appview/etzhayyim-wasm-systemofsystem-s0s5ys0s/svelte`
で、これはどちらにも当たらない。**この app は上流でも一度も installable では
なかった。** 上流の `pnpm-workspace.yaml` は自分のコメントでその結末を書いている
——「Without this, vitest / tsc / wrangler --dry-run fail to resolve `@etzhayyim/sdk*`」。

### ただし依存は失われていない（ここが直す取っ掛かり）

2 つとも、このワークスペース内に**別の名前で実在する**:

| package.json の `name` | 在り処 |
|---|---|
| `@etzhayyim/design-system` | `orgs/kotoba-lang/svelte-design-system`（v0.1.0） |
| `@etzhayyim/vite-plugin-safe-builder` | `orgs/kotoba-lang/vite-plugin-safe-builder`（v0.1.0） |

上流の `40-engine/svelte-MOVED.edn` が、両方を standalone repo へ出したと
記録している。**`workspace:*` のままでは届かない**ので、直すなら
参照の形（`file:` / git 依存 / workspace root の新設）を決める必要がある——
それは設計判断で、この README の仕事ではない。

## 3. 3 つの散文が言っていて、tree に無いもの

`docs/verify-docs-claims.cljk` が全部機械で再検査する。

| 出典 | 主張 | 実測 |
|---|---|---|
| `PROJECT.jsonld` | `hasPart.path` = `wasm/etzhayyim-wasm-systemofsystem-s0s5ys0s` | `wasm/` が無い |
| `PROJECT.jsonld` | `programmingLanguage: ["Go", "TypeScript"]` | `.go` が **0 ファイル** |
| `PROJECT.jsonld` | `runtimePlatform: "SpinKube + SvelteKit"` | Spin manifest 無し。deploy 設定も無し |
| `PROJECT.jsonld` | task「Threlte ベースの 3D systems map を設計」= **completed** | Threlte を import する行が **0** |
| `PROJECT.jsonld` | task「Arrow-schema-first natad / LanceDB manifest を定義」= **completed** | 該当 artifact が無い |
| `PROJECT.jsonld` | `url: https://sos.etzhayyim.com` | **NXDOMAIN** |
| `kotodama.jsonld` | `component.path: component.wasm` | そのファイルが無い |
| `kotodama.jsonld` | `triggers` が 3 つの `com.etzhayyim.apps.sos.*` collection を購読 | lexicon が 1 件も無い |
| `appview/README.md` | `etzhayyim-wasm-…/`: 「App shell and health endpoint」 | server コードが 1 行も無い |
| `appview/README.md` | `svelte/`: 「Threlte UI for the 3D system map」 | 22 行の placeholder |
| `AGENTS.md` | three.js + `@threlte/*` は「documented design intent, **NOT dead deps**」 | import 0 件、かつ**インストール自体ができない** |

最後の行が一番厄介で、この文は dead-deps 掃除から依存を守るために書かれている。
結果として、**解決できない依存が「消すな」と注記付きで保存されている。**

`tailwind.config.js` も同じ形で壊れている。1 行目が
`import { etzhayyimUIKit } from '@etzhayyim/design-system/plugin'` なので、
**config の読み込み時点で落ちる**（未使用なのではなく、評価に失敗する）。
`content` glob が指す `../../../../../packages/ts/design-system/dist/**` は
**上流にも存在しない**パスで、しかも同じ行が 2 回書かれている。
なお、どの CSS 入口も tailwind を読み込んでいないので、
postcss パイプライン自体そもそも走らない。

## 4. 正しかったもの

疑わしいものだけ並べると偏るので、**測って正しかった主張**も書く:

- **custody は完全。** 継承 14 ファイルは上流 `etzhayyim/root@afe5f1d9` の
  `60-apps/etzhayyim-project-sos`、tree `c86a9592…` と **blob SHA が 14/14 一致**し、
  合計は `migration.edn` が言うとおり **9,411 バイト**ちょうど。
  つまり上に並べた欠陥は**全部継承**であって、抽出が持ち込んだものは 1 つも無い。
- `README.edn` の `:graph-intelligence "cloud-itonami/graph-sos-intel"` は
  west に登録済み（pin `5db67cff…`）で解決する。
- `AGENTS.md` の「この app は `@etzhayyim/kami-engine-sdk` に依存しない」は正しい。
- `AGENTS.md` が挙げる 2 つの参照（兄弟 `etzhayyim-project-cad/AGENTS.md` と
  ADR-2605264300）は**どちらも上流に実在する**。参照が壊れているのではなく、
  現在地の記述が実態と合っていない。

## 5. 触る前に

- **`AGENTS.md` / `PROJECT.jsonld` / `appview/README.md` を現状の説明として読まない。**
  3 つとも継承物で、同じ内容が上流 `etzhayyim/root` にも在る。ここでは
  書き換えていない（custody を壊さないため）。この README が読み替え表。
- **最初の設計判断は「2 つの依存をどう参照するか」**（§2）。それが決まるまで、
  ビルドを前提にした作業は進まない。
- `docs/operator-quickstart.md` を 1 回踏めば、上の表は全部自分の手で再現できる。

