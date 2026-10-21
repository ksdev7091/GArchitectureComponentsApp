# 用語集

| 用語 | 意味 | このリポジトリでの例 |
|---|---|---|
| 関心の分離 (Separation of concerns) | 「描く」「データを取る」「業務ルール」などの責務を別のクラスに分けること | 01 の `ui/` `domain/` `data/` パッケージ |
| UI 状態 (UI state) | 画面を描くのに必要な情報をまとめた不変オブジェクト | `NotesUiState`, `TodoUiState` |
| 状態ホルダー (State holder) | UI 状態を保持・更新する部品。画面単位なら ViewModel | `NotesViewModel` |
| 単方向データフロー (UDF) | 状態は上(ViewModel)から下(UI)へ、イベントは下から上へ、一方向にだけ流す設計 | `NotesScreen` と `NotesViewModel` の関係 |
| 状態ホイスティング (State hoisting) | Composable から状態を引数に引き上げ、Composable 自体を状態なし(stateless)にすること | `NotesScreen(uiState, onXxx...)` |
| 唯一の信頼できる情報源 (SSOT) | あるデータについて「これが正」と言える 1 か所 | Room (`NotesDatabase`) |
| オフラインファースト | ローカル DB を SSOT にし、ネットワークからの取得は DB への書き込みとして扱う設計 | `OfflineFirstNotesRepository` |
| Repository | 1 種類のデータについて、取得・変更の公開 API を提供する部品 | `NotesRepository` |
| DataSource | 1 つの出どころ(DB、API、ファイル)だけを扱う部品 | `NoteDao`, `NoteRemoteDataSource` |
| UseCase | 1 つの業務操作を表す、Domain 層の部品 | `GetNotesUseCase` |
| main-safe | メインスレッドから呼んでも UI を止めない(内部で適切にスレッドを切り替える) | Repository のすべての関数 |
| 依存性の注入 (DI) | 必要なものを自分で new せず、外から受け取る設計。組み立てを 1 か所に集める | コンストラクタ引数、`AppContainer`, Hilt |
| コンストラクタインジェクション | 依存をコンストラクタ引数で受け取る形 | `class NotesViewModel @Inject constructor(...)` |
| Fake | テスト用に、インターフェースを本物らしく軽量に実装したもの | `FakeNotesRepository` |
| 構成変更 (Configuration change) | 画面回転・言語変更などで Activity が破棄・再生成されること | ViewModel が生き残る理由 |
| プロセス再生成 (Process death) | バックグラウンドのアプリの OS が kill し、復帰時に作り直されること | `SavedStateHandle` が必要な理由 |
| `SavedStateHandle` | プロセス再生成を生き延びる小さな key-value ストア。ViewModel に注入される | `NoteEditorViewModel` の入力中テキスト |
| `StateFlow` | 常に最新値を 1 つ持つ Flow。UI 状態の公開に使う | `uiState: StateFlow<NotesUiState>` |
| `stateIn` | 普通の Flow を StateFlow に変換する。`WhileSubscribed(5_000)` で購読者がいる間だけ上流を動かす | `NotesViewModel.uiState` |
| `collectAsStateWithLifecycle` | Compose で StateFlow を購読する。画面が見えていない間は購読を止める | `NotesScreen` |
| `repeatOnLifecycle` | View システムで Flow を購読する。指定の Lifecycle 状態の間だけ collect する | Step 2〜4 の `MainActivity` |
| Entity / DTO / モデル | DB の行 / API の応答 / アプリ内部の表現。層ごとに分けて Data 層で変換する | `NoteEntity` / `NetworkNote` / `Note` |
