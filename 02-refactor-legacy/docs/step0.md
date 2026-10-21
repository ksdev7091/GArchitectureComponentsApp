# Step 0: 出発点 — よくある古い Activity

モジュール: `step0-legacy`

## コードを読む前に

まず [MainActivity.kt](../step0-legacy/src/main/java/com/example/legacytodo/MainActivity.kt) を**解説を読まずに**読んでください。
一見ちゃんと動きます。実際、動きます。「このコードの何が問題か」を自分の言葉で 3 つ挙げてから先に進んでください。

## 構成

```
step0-legacy/src/main/java/com/example/legacytodo/
├── MainActivity.kt    画面 + DB アクセス + スレッド + 業務ロジック + Adapter(約 150 行)
├── TodoDbHelper.kt    SQLiteOpenHelper
└── Todo.kt            data class
```

## 問題点(コード中の ①〜⑥ に対応)

| # | 問題 | 何が起きるか | 反している原則 |
|---|---|---|---|
| ① | Activity が全部やっている | 1 ファイルが肥大化し、修正の影響範囲が読めない | 関心の分離 |
| ② | 生の `Thread` + `runOnUiThread` | Thread が破棄済みの Activity を掴んだまま走り続ける(リーク)。`onDestroy` の `dbHelper.close()` と別スレッドの DB アクセスが競合するとクラッシュ | Activity を信用しない |
| ③ | 状態が Activity のフィールド | 回転のたびに DB を読み直す。読み直し中は一瞬空になる | データモデルから UI を駆動する |
| ④ | 業務ロジックが UI に埋まっている | 「完了数の集計」「空文字チェック」をテストするには画面を起動するしかない | 関心の分離 |
| ⑤ | 依存を自分で `new` している | `TodoDbHelper` を差し替える手段がなく、テストで本物の DB が必要 | DI |
| ⑥ | テストが書けない | ①〜⑤の帰結 | — |

### もう少し深く: なぜ「動くのに問題」なのか

このコードは**今日**は動きます。問題は**変更するとき**に現れます。

- 「完了済みを非表示にするフィルタを足したい」→ `loadTodos()` の SQL を変える? `todos` を絞る? `updateSummary()` の集計はフィルタ前の件数? フィルタ後? 全部 Activity の中なので、影響範囲が読めない。
- 「サーバーと同期したい」→ Activity に `HttpURLConnection` も足す? スレッドがもう 1 本増える。
- 「タブレット用に 2 ペインにしたい」→ Fragment に分けようとすると、`todos` と DB アクセスを両方の Fragment が必要とし、同じコードが 2 か所に。
- 「集計ロジックが間違っていた」→ 直したあと正しいことをどう確かめる? エミュレータで手動で数える。

アーキテクチャは「今日動くか」ではなく「明日変えられるか」のためにあります。

なお、DB アクセスのエラー処理(例外が出たときにどう知らせるか)は Step 0〜5 を通して扱いません。
「例外を状態に変換する」パターンは [01-new-app の NotesViewModel.refresh()](../../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesViewModel.kt) を参照してください。

## 書き込み後の手動再読込

もう 1 つ注目してほしいのは、`addTodo` / `setDone` / `deleteTodo` がすべて最後に `loadTodos()` を呼んでいることです。
「DB を変えたら画面も更新しなければならない」ことを**プログラマが覚えていなければならない**。
1 か所でも忘れると、DB と画面がずれます。
これを「DB が変われば勝手に画面が変わる」形に変えるのが Step 3 の目的です。

## 次のステップ

[Step 1](step1.md): DB アクセスとスレッド管理を Activity から追い出します。
