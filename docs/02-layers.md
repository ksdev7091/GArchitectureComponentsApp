# 02. 各層の責務とルール

[01-architecture-mindset.md](01-architecture-mindset.md) の原則を、層ごとの具体的なルールに落とします。
各ルールには「このリポジトリのどこで見られるか」を添えています。

## UI 層 (UI layer)

**役割**: アプリのデータを画面に表示し、ユーザー操作をイベントとして受け取る。

UI 層は 2 種類の部品でできています。

| 部品 | 担当 | 例 |
|---|---|---|
| UI 要素 | 描画そのもの | Compose の `@Composable` 関数、XML + View |
| 状態ホルダー | 状態を持ち、ロジックを扱う | `ViewModel` |

### ルール

1. **UI 状態は 1 つの不変な data class にまとめる**
   画面に必要なものすべて(リスト、読込中フラグ、エラー)を `XxxUiState` に入れる。
   不変(`val`)なので「いつの間にか書き換わった」が起きない。更新は `copy()` で新しいインスタンスを作る。
   → [NotesUiState](../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesViewModel.kt)
2. **ViewModel は Android の View/Context を参照しない**
   参照した瞬間に JVM 単体テストができなくなる。文字列が必要なら `@StringRes Int` を状態に入れ、文字列化は UI でやる。
   → `NotesUiState.userMessageRes`
3. **ViewModel は `StateFlow<UiState>` を公開し、UI はそれを購読する**
   Compose なら `collectAsStateWithLifecycle()`、View なら `repeatOnLifecycle(STARTED)` で、画面が見えている間だけ購読する。
   → [NotesScreen](../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesScreen.kt), [Step 2 の MainActivity](../02-refactor-legacy/step2-viewmodel/src/main/java/com/example/legacytodo/MainActivity.kt)
4. **一度きりの通知(Snackbar、画面遷移)も状態で表す**
   「表示すべきメッセージがある」という状態を持ち、UI が受け取ったら `userMessageShown()` で消してもらう。
   SharedFlow や Channel で「イベント」として別に流す設計は、配信と処理が保証されない
   (購読していない瞬間に失われる、UI が処理できない状態のときに届く)ため、公式に非推奨になった。
   `isSaved` のように画面が閉じて ViewModel ごと破棄される状態は、消してもらう呼び出しが不要。
   → `NotesViewModel.userMessageShown()`, `NoteEditorUiState.isSaved`
5. **Composable は「状態あり版」と「状態なし版」に分ける(状態ホイスティング)**
   状態あり版は ViewModel とつなぐだけ。状態なし版は引数の UiState を描くだけ。
   状態なし版は Preview とテストで任意の状態を直接渡せる。
   → [NotesScreen.kt](../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesScreen.kt) の 2 つの `NotesScreen` 関数
6. **入力途中のテキストは、置き場所を 2 つの基準で決める**
   - ViewModel の処理(保存など)がその値を必要とする → ViewModel に置き、`savedStateHandle.saveable` で
     プロセス再生成を生き延びさせる(01 の編集画面)。このとき StateFlow ではなく
     スナップショット状態(`mutableStateOf`)にする。TextField は入力が同期的に反映されることを前提に
     しており、StateFlow を経由すると速い入力や日本語変換中に文字が欠けることがある。
   - イベントの引数として一度渡すだけで ViewModel が保持しない → Composable の `rememberSaveable`(02 Step 5 の追加欄)。
   どちらも回転とプロセス再生成を生き延びる。SavedStateHandle は Bundle に入るので、置く状態は小さく保つ。
   → [NoteEditorViewModel](../01-new-app/app/src/main/java/com/example/archnotes/ui/editor/NoteEditorViewModel.kt)
7. **画面(Screen)は遷移先を知らない**
   `onNoteClick: (Long) -> Unit` のようなコールバックを受け取り、実際の遷移は NavHost が決める。
   ルート定義と NavGraph への登録関数は画面側のパッケージに置き、NavHost がそれに依存する(逆は不可)。
   → [ArchNotesNavHost](../01-new-app/app/src/main/java/com/example/archnotes/ui/ArchNotesNavHost.kt),
     [NoteEditorNavigation](../01-new-app/app/src/main/java/com/example/archnotes/ui/editor/NoteEditorNavigation.kt)

### 状態ホルダーの種類

- **ViewModel**: 画面(または遷移先)単位の状態。構成変更を生き延びる。業務ロジックを扱う。
- **プレーンな状態ホルダークラス**: UI 要素の複雑な状態(スクロール位置、展開/折りたたみなど)。Compose の `remember` で保持。
- 迷ったら「業務ロジックに関係するか?」で分ける。関係するなら ViewModel。
  → Step 5 の `TodoScreen` では入力中テキストを `rememberSaveable` で UI 側に置いている。空文字チェック(業務ルール)は ViewModel にある。

## Domain 層 (Domain layer) — 省略可能

**役割**: 複雑な業務ロジック、または複数の ViewModel で再利用する業務ロジックを UseCase にまとめる。

### ルール

1. **置くのは次のどちらかに当てはまるときだけ**
   - 複数の ViewModel が同じロジックを必要とする
   - ViewModel が肥大化してきたので、ロジックを切り出して単独でテストしたい
2. **命名は「動詞 + 名詞 + UseCase」、公開メソッドは `operator fun invoke` 1 つ**
3. **UseCase は状態を持たない。Repository に依存し、UI には依存しない**
4. **UseCase は他の UseCase に依存してよい**(階層にする)

→ [GetNotesUseCase](../01-new-app/app/src/main/java/com/example/archnotes/domain/GetNotesUseCase.kt)
   メモの Repository と設定の Repository を `combine` して「検索 → ユーザーの並び順で並べ替え」を行う。
   2 つの Repository の組み合わせ方を ViewModel に書くと一覧画面の ViewModel が肥大化し、別の画面で同じ一覧が要るときに複製することになる。
   02-refactor-legacy には Domain 層がありません。Todo アプリの規模では不要だからです。これも正しい判断です。

## Data 層 (Data layer)

**役割**: アプリのデータの「真実」を保持し、公開し、変更する。業務ロジックのうち「データに関するもの」(キャッシュ戦略、同期、整合性)もここ。

Data 層は 2 種類の部品でできています。

| 部品 | 担当 | 例 |
|---|---|---|
| Repository | 1 種類のデータについての公開 API。複数の DataSource を組み合わせる | `NotesRepository`, `UserPreferencesRepository` |
| DataSource | 1 つの出どころ(DB、API、ファイル、DataStore)だけを扱う | `NoteDao`(Room), `NoteRemoteDataSource`, `DataStore<Preferences>` |

### ルール

1. **Repository はインターフェースにし、実装と分ける**
   UI/Domain 層はインターフェースだけに依存する。テストでは Fake を差し込む。
   → [NotesRepository](../01-new-app/app/src/main/java/com/example/archnotes/data/NotesRepository.kt) と [OfflineFirstNotesRepository](../01-new-app/app/src/main/java/com/example/archnotes/data/OfflineFirstNotesRepository.kt)
2. **読み取りは `Flow`、書き込みは `suspend`**
   Flow なら「変わったら流れてくる」ので呼び出し側が再取得を管理しなくていい。
   → [Step 3 の変更点](../02-refactor-legacy/docs/step3.md) がこの効果を実感できる
3. **main-safe にする**
   Repository の関数はメインスレッドから呼んでも安全でなければならない。スレッドの切り替えは Repository か DataSource の責任。
   Room と Retrofit の suspend 関数はもともと main-safe なので、それらを呼ぶだけの Repository に `withContext` は要らない。
   ブロッキングな API を使うなら、それを包む DataSource の中で `withContext(Dispatchers.IO)` を書き、上の層には漏らさない。
   → [OfflineFirstNotesRepository](../01-new-app/app/src/main/java/com/example/archnotes/data/OfflineFirstNotesRepository.kt) のクラスコメント
4. **SSOT を決める**
   オフライン対応するなら Room を SSOT にし、サーバーの応答は一度 Room に書いてから UI に届ける(オフラインファースト)。
5. **層ごとにモデルを分ける**
   DB の行(`NoteEntity`)、API の応答(`NetworkNote`)、アプリのモデル(`Note`)は別のクラスにし、Data 層の中で変換する。
   DB のカラム名を変えても UI 層が影響を受けない。
   サーバー側の ID とローカル DB の自動採番 ID は別のカラムに持つ。1 つの列に混ぜると衝突してユーザーのデータが黙って上書きされる。
   → [NoteEntity.kt](../01-new-app/app/src/main/java/com/example/archnotes/data/local/NoteEntity.kt) の `remoteId` と `asModel()` / `asEntity()`
6. **DataSource は Repository 以外から呼ばない**
   ViewModel が DAO を直接触り始めると、Repository の存在意義(SSOT、キャッシュ戦略)が崩れる。
7. **エラーは例外で投げ、ViewModel が状態に変換する**
   DataSource は「通信失敗は `IOException`」のような契約を決め、実装固有の例外(Retrofit の `HttpException` など)は
   DataSource の中で契約の例外に変換する。ViewModel はその契約に合わせて catch し、`userMessageRes` のような状態にする。
   `Result` 型を使う流儀もあるが、どちらにせよ「UI まで例外を素通しにしない」「実装を替えても catch 節が変わらない」のが要点。
   → [NoteRemoteDataSource](../01-new-app/app/src/main/java/com/example/archnotes/data/remote/NoteRemoteDataSource.kt) のクラスコメント

## 依存性の注入 (DI) — 層をつなぐ接着剤

層を分けると「誰が誰を new するか」という問題が残ります。
Activity が `SqliteTodoRepository(TodoDbHelper(this))` と組み立てるなら、Activity が Data 層の内部構造を知ってしまう。

解決策は **組み立てを 1 か所に集める** ことです。

- 手動 DI: `AppContainer` クラスを Application に持たせる → [Step 2](../02-refactor-legacy/step2-viewmodel/src/main/java/com/example/legacytodo/TodoApplication.kt)
- Hilt: アノテーションから AppContainer 相当を自動生成する → [Step 4](../02-refactor-legacy/step4-hilt-test/src/main/java/com/example/legacytodo/di/)

各クラスは「自分が必要とするもの」をコンストラクタ引数で宣言するだけ(コンストラクタインジェクション)。
これにより、テストでは別の実装(Fake)を同じ引数に渡せます。

## 層をまたぐときのチェックリスト

- ViewModel が `Context` / `View` / `Activity` を import していないか
- Composable / Activity が Repository や DAO を直接呼んでいないか
- Repository の公開メソッドがすべて `Flow` か `suspend` か
- UI 層に Room の `@Entity` や API の DTO が漏れていないか
- 状態を変更しているのが ViewModel だけか(UI が `mutableStateOf` で業務データを持っていないか)

次: [03-decision-guide.md](03-decision-guide.md) で、迷いどころの判断基準を見ます。
