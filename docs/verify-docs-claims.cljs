#!/usr/bin/env nbb
;; verify-docs-claims — README.md が主張する測定を tree に対して再検査する。
;;
;; 終了コードは 3 つある。0/1 の 2 値にしない:
;;
;;   0  すべての claim が現在の tree と一致した
;;   1  一致しない claim がある（= README が古い、または repo が変わった）
;;   3  **測れなかった**（repo 外で実行された / git が引けない / 証拠が床を割った）
;;
;; 3 が別値なのは、ADR-2608136000 の「測れなかった検査が、測って問題が
;; 無かった検査と同じ値を返す」を避けるため。ここで 0 を返すと、
;; 何も読めなかった run が「異常なし」として蓄積する。
;;
;; 使い方: nbb docs/verify-docs-claims.cljs        （repo ルートで）

(ns verify-docs-claims
  (:require [clojure.string :as str]
            ["node:fs" :as fs]
            ["node:child_process" :as cp]))

(def ^:private APP "appview/etzhayyim-wasm-systemofsystem-s0s5ys0s")

;; 継承 14 ファイルの blob SHA。上流 etzhayyim/root@afe5f1d9 の
;; 60-apps/etzhayyim-project-sos（tree c86a9592…）と 14/14 一致した時点の値。
(def ^:private INHERITED
  [["CLAUDE.md" "f0be9348b946a9270ae3ba69be21b089148d21c2"]
   ["NOTICE" "d41cc9adaaf43edb0f4493d1bd7fdcbb77f77281"]
   ["PROJECT.jsonld" "87c4cdc0be9d175458112c1c989fbfbbf86879c2"]
   ["appview/README.md" "92e8061562b36f578b92cfbfd2ad1c172c39ab56"]
   [(str APP "/kotodama.jsonld") "0e072f186aaa69247df1dfe0c22b5acba4b0e24e"]
   [(str APP "/svelte/index.html") "44556fbc4f807ccce018224b84eed52515fa35bd"]
   [(str APP "/svelte/package.json") "16e7286f0099011fb0de1e991284364c89fb5ef3"]
   [(str APP "/svelte/postcss.config.js") "2aa7205d4b402a1bdfbe07110c61df920b370066"]
   [(str APP "/svelte/src/App.svelte") "378331ee9a1139dce45facd4c717ead195d1c20f"]
   [(str APP "/svelte/src/main.ts") "765afde7c320079138ad328f24180a41d9b5f4af"]
   [(str APP "/svelte/src/svelte.d.ts") "fed7ce9f49b2d91dc982ae58c29b6d42d9494a30"]
   [(str APP "/svelte/tailwind.config.js") "058ebb0191e7d78cff46bb5d62808ef5bdef5884"]
   [(str APP "/svelte/tsconfig.json") "8a47c403495ef987418402af062e3ece5bb82f4f"]
   [(str APP "/svelte/vite.config.ts") "a14343d911fa9e29c187fdd96b34b655f5a332b4"]])

(def ^:private INHERITED-BYTES 9411)   ; migration.edn :source :bytes
(def ^:private TRACKED-TOTAL 16)       ; 14 継承 + README.edn + migration.edn
;; README.md / docs/ はこの検査自身が足したもの。custody の数からは外す。
(def ^:private ADDED #{"README.edn" "migration.edn"})

;; ---------------------------------------------------------------- 測れない

(defn- cannot!
  "測れなかった。0 でも 1 でもない 3 で降りる。理由は必ず本文ごと出す。"
  [why]
  (println (str "CANNOT-MEASURE\t" why))
  (println "  → これは pass ではない。exit 3。")
  (js/process.exit 3))

(defn- sh
  "git を引く。失敗したら **stderr の本文ごと** cannot! に渡す（status を捨てない）。"
  [& args]
  (let [r (cp/spawnSync "git" (clj->js (into ["-c" "core.fsmonitor=false"] args))
                        #js {:encoding "utf8"})]
    (when (.-error r) (cannot! (str "git を起動できない: " (.-message (.-error r)))))
    (when-not (zero? (.-status r))
      (cannot! (str "git " (str/join " " args) " が exit " (.-status r)
                    " / stderr: " (str/trim (or (.-stderr r) "(空)")))))
    (str/trim (or (.-stdout r) ""))))

(defn- slurp* [p]
  (try (fs/readFileSync p "utf8") (catch :default _ nil)))

;; ---------------------------------------------------------------- 検査

(def ^:private results (atom []))

(defn- check! [claim ok? detail]
  (swap! results conj {:claim claim :ok? (boolean ok?) :detail detail}))

(defn -main []
  ;; 前提条件。ここで 3 に落ちるのは「この repo の中に居ない」場合だけ。
  (when-not (and (fs/existsSync "migration.edn") (fs/existsSync APP))
    (cannot! (str "app-sos の repo ルートで実行されていない（migration.edn か "
                  APP " が見えない）。cwd=" (js/process.cwd))))

  (let [tracked (->> (sh "ls-files") str/split-lines (remove str/blank?) vec)
        blobs   (->> (sh "ls-tree" "-r" "HEAD") str/split-lines
                     (remove str/blank?)
                     (map (fn [l] (let [[meta path] (str/split l #"\t" 2)
                                        sha (nth (str/split meta #"\s+") 2 nil)]
                                    [path sha])))
                     (into {}))]

    (when (empty? tracked) (cannot! "git ls-files が 0 件を返した（tree が読めていない）"))
    (when (empty? blobs)   (cannot! "git ls-tree が 0 件を返した（HEAD が読めていない）"))

    ;; ── custody: 継承 14 ファイルが 1 バイトも動いていないこと ──────────
    (doseq [[path sha] INHERITED]
      (let [actual (get blobs path)]
        (check! (str "継承 " path)
                (= actual sha)
                (cond (nil? actual) (str "tree に無い（消えている）")
                      (not= actual sha) (str "blob が違う: " (subs actual 0 12)
                                             " ≠ " (subs sha 0 12))
                      :else "blob 一致"))))

    (check! "継承ファイルの合計バイト = migration.edn の :bytes"
            (= INHERITED-BYTES
               (reduce + 0 (map (fn [[p _]]
                                  (if-let [t (slurp* p)]
                                    (.-length (js/Buffer.from t "utf8"))
                                    0))
                                INHERITED)))
            (str "期待 " INHERITED-BYTES))

    (check! "抽出時の追加は README.edn / migration.edn の 2 つだけ"
            (= ADDED (into #{} (remove (into #{} (map first INHERITED))
                                       (remove #(or (= % "README.md")
                                                    (str/starts-with? % "docs/"))
                                               tracked))))
            (str "継承 " (count INHERITED) " + 追加 2 = " TRACKED-TOTAL))

    ;; ── ビルド不能の根拠 ────────────────────────────────────────────
    (let [pkg (slurp* (str APP "/svelte/package.json"))]
      (when-not pkg (cannot! (str "package.json が読めない: " APP "/svelte/package.json")))
      (let [ws (set (map second (re-seq #"\"(@etzhayyim/[^\"]+)\":\s*\"workspace:\*\"" pkg)))]
        (check! "workspace:* の依存はちょうど 2 つ"
                (= ws #{"@etzhayyim/design-system" "@etzhayyim/vite-plugin-safe-builder"})
                (str "実測: " (pr-str (sort ws))))))

    (check! "この repo に workspace root が無い（だから workspace:* が解決しない）"
            (not-any? #(re-find #"(?i)^(pnpm-workspace\.ya?ml|package\.json|pnpm-lock\.ya?ml|\.npmrc)$" %)
                      tracked)
            "ルートの package.json / pnpm-workspace.yaml / lockfile はいずれも無い")

    ;; ── 散文が言っていて tree に無いもの ───────────────────────────
    (check! "wasm/ が無い（PROJECT.jsonld hasPart.path の主張）"
            (not-any? #(str/starts-with? % "wasm/") tracked) "")
    (check! "component.wasm が無い（kotodama.jsonld component.path の主張）"
            (not (fs/existsSync (str APP "/component.wasm"))) "")
    (check! "Go のソースが 0 件（PROJECT.jsonld programmingLanguage の主張）"
            (zero? (count (filter #(str/ends-with? % ".go") tracked))) "")
    (check! "Spin manifest が無い（runtimePlatform: SpinKube の主張）"
            (not-any? #(re-find #"(?i)(^|/)spin(app)?\.(toml|ya?ml)$" %) tracked) "")
    (check! "deploy 設定が無い"
            (not-any? #(re-find #"(?i)wrangler|dockerfile|(^|/)k8s/" %) tracked) "")
    (check! "lexicon が 0 件（kotodama.jsonld triggers が 3 collection を購読）"
            (not-any? #(re-find #"(?i)lexicon" %) tracked) "")

    ;; ── Threlte: 宣言されているが 1 行も使われていない ─────────────
    (let [srcs (filter #(str/starts-with? % (str APP "/svelte/src/")) tracked)]
      (when (empty? srcs) (cannot! "svelte/src/ 配下のファイルが 1 つも見えない"))
      (check! "svelte/src と index.html に threlte/three の import が 0 件"
              (not-any? (fn [f] (when-let [t (slurp* f)]
                                  (re-find #"(?i)threlte|['\"]three['\"]" t)))
                        (conj (vec srcs) (str APP "/svelte/index.html")))
              (str "走査 " (inc (count srcs)) " ファイル")))

    ;; ── tailwind.config.js は評価に失敗する ────────────────────────
    (let [tw (slurp* (str APP "/svelte/tailwind.config.js"))]
      (when-not tw (cannot! "tailwind.config.js が読めない"))
      (check! "tailwind.config.js が解決できない package を import している"
              (re-find #"import\s+\{[^}]*\}\s+from\s+'@etzhayyim/design-system/plugin'" tw)
              "1 行目で落ちる（未使用ではなく評価に失敗する）")
      (check! "tailwind content glob が同じ dangling パスを 2 回書いている"
              (= 2 (count (re-seq #"\.\./\.\./\.\./\.\./\.\./packages/ts/design-system/dist" tw)))
              "上流 etzhayyim/root にも存在しないパス"))

    ;; ── README.md がこの検査と同じ数を言っていること ────────────────
    (let [rd (slurp* "README.md")]
      (when-not rd (cannot! "README.md が読めない（この検査が守る対象が無い）"))
      (doseq [[label needle] [["継承 14 ファイル" "14"]
                              ["9,411 バイト" "9,411"]
                              ["35 個の glob" "35 個の glob"]
                              ["追跡 16" "16"]]]
        (check! (str "README.md が " label " と書いている")
                (str/includes? rd needle) "")))

    ;; ── 証拠の床。数えていないのに clean と言わせない ────────────────
    (let [n (count @results)
          bad (filter (complement :ok?) @results)]
      (when (< n 31)
        (cannot! (str "証拠が床を割った: claim を " n " 件しか評価していない（床 31）。"
                      " check! が黙らされたか、走査対象が消えている")))
      (println (str "SCANNED\t" (count tracked)))
      (println (str "CLAIMS\t" n))
      (doseq [{:keys [claim ok? detail]} @results]
        (when-not ok?
          (println (str "FAIL\t" claim (when (seq detail) (str "\t" detail))))))
      (if (seq bad)
        (do (println (str "RESULT\tFAIL " (count bad) "/" n))
            (js/process.exit 1))
        (do (println (str "RESULT\tOK " n "/" n " claims match the tree"))
            (js/process.exit 0))))))

(-main)
