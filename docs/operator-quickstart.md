# operator quickstart — app-sos

**所要 3 分。** この repo について README.md が言っていることを、
**全部その場で自分で測り直す**ための手順。読んで納得するのではなく、
1 行ずつ実行して出力を見る。

前提は `git` と `nbb` だけ。**§5–§7 だけは west workspace
（superproject `com-junkawasaki/root`）が要る** —— この repo の外を見るため。
手元に無ければその 3 節は飛ばしてよく、飛ばしたことが分かるように書いてある。

以下すべて **repo のルート**（`migration.edn` が見えるところ）で実行する。

---

## §1 いま何を持っているか

```bash
git ls-files | wc -l          # → 19
```

19 のうち **16 が抽出時の姿**（継承 14 + 許された追加 2）で、
残る 3 つ（`README.md` と `docs/` の 2 つ）は**この文書群が後から足したもの**。
抽出時の追加が `migration.edn` の `:allowed-additions` とちょうど一致することを見る:

```bash
git ls-files \
  | grep -vE '^(CLAUDE\.md|NOTICE|PROJECT\.jsonld|appview/)' \
  | grep -vE '^(README\.md|docs/)'
```

→ `README.edn` と `migration.edn` の **2 行だけ**。

実行されるコードはこれだけ:

```bash
wc -l appview/etzhayyim-wasm-systemofsystem-s0s5ys0s/svelte/src/*
```

→ App.svelte 22 行 / main.ts 6 行 / svelte.d.ts 5 行 —— 合計 **33 行**。

## §2 custody —— 継承 14 ファイルは 1 バイトも動いていない

`migration.edn` は上流とバイト数まで言い切っている。合計を出して突き合わせる:

```bash
git ls-files -- CLAUDE.md NOTICE PROJECT.jsonld appview \
  | xargs wc -c | tail -1
```

→ `9411`。`migration.edn` の `:source :bytes` と一致する:

```bash
grep -o ':bytes [0-9]*' migration.edn
```

**ここが一致することが、以降の欠陥を「継承」と呼べる根拠**である。
blob SHA 単位の照合は §5 で行う（上流の checkout が要る）。

## §3 ビルドできないことを再現する

`workspace:*` を使っている依存を挙げる:

```bash
grep -n 'workspace:\*' appview/*/svelte/package.json
```

→ `@etzhayyim/design-system` と `@etzhayyim/vite-plugin-safe-builder` の 2 つ。

`workspace:*` は「同じ pnpm workspace の兄弟を使う」という意味なので、
workspace root が要る。**この repo には無い**:

```bash
ls pnpm-workspace.yaml package.json pnpm-lock.yaml 2>&1
```

→ 3 つとも `No such file or directory`。

registry に落ちることもできない。**両方 404**:

```bash
for p in '@etzhayyim%2fdesign-system' '@etzhayyim%2fvite-plugin-safe-builder'; do
  printf '%s -> ' "$p"
  curl -s -o /dev/null -w '%{http_code}\n' "https://registry.npmjs.org/$p"
done
```

→ `404` / `404`。

> パッケージ名を URL に入れるときは `@` を `%2f` 込みで
> エスケープしてある形をそのまま使うこと。`curl -w` の値を
> `@` で始めると **curl はそれをファイル名として読む**ので、
> 測ったつもりで usage error を見ることになる。

## §4 tailwind は「未使用」ではなく「評価に失敗する」

```bash
head -1 appview/*/svelte/tailwind.config.js
```

→ `import { etzhayyimUIKit } from '@etzhayyim/design-system/plugin';`
§3 で 404 を見た、まさにそのパッケージ。**config の読み込み時点で落ちる。**

content glob も見る:

```bash
grep -n 'packages/ts/design-system' appview/*/svelte/tailwind.config.js
```

→ **同じ行が 2 回**。しかもこのパスは上流にも無い（§5 で確認する）。

なお、どの CSS 入口も tailwind を読み込んでいないので、
そもそも postcss は走らない:

```bash
grep -rn 'tailwind\|\.css' appview/*/svelte/src/ appview/*/svelte/index.html || echo '(CSS 入口が無い)'
```

## §5 上流を見る（west workspace が要る）

superproject のルートを控える。**無ければ §5–§7 は飛ばす**:

```bash
ROOT=~/github/com-junkawasaki
[ -d "$ROOT/orgs/etzhayyim/root" ] && echo "上流あり" || echo "SKIP: 上流 checkout が無い"
```

blob 単位の custody 照合（14/14 一致するはず）:

```bash
REV=afe5f1d995162277d2fb98f762705b53f9f695fd
diff <(git -C "$ROOT/orgs/etzhayyim/root" ls-tree -r \
         "$REV" -- 60-apps/etzhayyim-project-sos \
       | awk '{print $3}' | sort) \
     <(git ls-tree -r HEAD -- CLAUDE.md NOTICE PROJECT.jsonld appview \
       | awk '{print $3}' | sort) \
  && echo "custody OK: 14/14 blob 一致"
```

**この app は上流でも installable ではなかった**ことを見る。
`pnpm-workspace.yaml` の glob を全部出して、この app に当たるものを探す:

```bash
git -C "$ROOT/orgs/etzhayyim/root" show "$REV:pnpm-workspace.yaml" \
  | grep -cE '^\s+- "'                      # → 35
git -C "$ROOT/orgs/etzhayyim/root" show "$REV:pnpm-workspace.yaml" \
  | grep -iE 'sos|systemofsystem' || echo "→ 35 個のどれも sos に言及していない"
```

ワイルドカードは 2 本あるが、どちらも別の場所を指す:

```bash
git -C "$ROOT/orgs/etzhayyim/root" show "$REV:pnpm-workspace.yaml" | grep -E 'project-\*'
```

→ `…/kotoba` と `…/xrpc-adapter`。必要なのは
`…/etzhayyim-project-sos/appview/etzhayyim-wasm-systemofsystem-s0s5ys0s/svelte` で、
**どちらにも当たらない。**

§4 の content glob が上流にも無いことも、ここで確認できる:

```bash
git -C "$ROOT/orgs/etzhayyim/root" ls-tree "$REV" -- packages/ts/design-system \
  | grep . || echo "→ 上流にも packages/ts/design-system は無い"
```

## §6 依存は失われていない（west workspace が要る）

2 つとも別名で実在する。`name` を直接読む:

```bash
for r in svelte-design-system vite-plugin-safe-builder; do
  printf '%-28s ' "$r"
  grep -m1 '"name"' "$ROOT/orgs/kotoba-lang/$r/package.json" 2>/dev/null \
    || echo '(未 checkout — west update --fetch smart '"$r"' で取る)'
done
```

→ `@etzhayyim/design-system` / `@etzhayyim/vite-plugin-safe-builder`。

**手元に無いことは存在しないことではない。** west は 4,000 超の repo を
管理していて、checkout していないものは `ls` にも `find` にも映らない。
上の 2 つは名前に `etzhayyim` も `design-system` の完全形も含まないので、
`repo-search` で引くのが確実:

```bash
(cd "$ROOT" && nbb scripts/repo-search.cljs design-system safe-builder) | head -20
```

> `repo-search` は `manifest/west.yml` を **cwd から**探すので、
> superproject のルートで実行する必要がある。スクリプトのパスだけを
> 絶対で渡しても `ENOENT: … /manifest/west.yml` になる。

## §7 宣言されたホスト（ネットワークが要る）

```bash
dig +short sos.etzhayyim.com A | grep . || echo "sos.etzhayyim.com → NXDOMAIN"
dig +short etzhayyim.com A | head -2
```

→ 前者は解決しない（`CLAUDE.md` は "(planned)" と正しく注記しているが、
`PROJECT.jsonld` の `url` は注記なしでこのホストを指している）。後者は解決する。

## §8 機械で再検査する

ここまで手で見たものを、まとめて再検査する:

```bash
nbb docs/verify-docs-claims.cljs
```

→ `RESULT  OK 31/31 claims match the tree`、exit 0。

終了コードは **3 値**である:

| exit | 意味 |
|---|---|
| 0 | 31 件すべて tree と一致した |
| 1 | 一致しない claim がある（どれかを名指しする） |
| **3** | **測れなかった** —— repo の外で実行した / git が引けない / 評価した claim が床（31）を割った |

**3 が 0 と別なのが要点。** 何も読めなかった run が「異常なし」として
積み上がらないようにしてある。試すなら:

```bash
V="$PWD/docs/verify-docs-claims.cljs"
(cd /tmp && nbb "$V"); echo "exit=$?"    # → 3
```

（`| head` などを挟むと `$?` が **パイプ末尾の**終了コードになり、
3 を見たつもりで 0 を見ることになる。素で実行して確かめること。）

