# 10 Java / Spring Boot実装規約
## 推奨パッケージ
`com.example.contracts` 配下に `contract`, `party`, `approval`, `document`, `security`, `audit`, `common` の機能別パッケージを置く。各機能に controller/service/repository/dto/entity を配置。
## Entity例
```java
@Entity
@Table(name = "contracts")
public class Contract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "contract_no", nullable = false, unique = true)
    private String contractNo;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "start_date")
    private LocalDate startDate;
}
```
省略したimport/getter等は実装時に追加する。公開setterを無制限に設けず、ドメイン操作メソッドを優先する。
## DTO例
```java
public record ContractCreateRequest(
    @NotBlank String contractNo,
    @NotBlank String name,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @DecimalMin("0.00") BigDecimal amount
) {}
```
## 注意点
- `jakarta.persistence` / `jakarta.validation` を使用する。
- `@RestControllerAdvice` と `ProblemDetail` で共通例外処理。
- Serviceで状態遷移を検証、`@Transactional` を設定。承認は `ApprovalService` に集約し、先頭 PENDING / 後続 WAITING、差戻しコメント、取下げ、固定版参照をここで完結させる。
- 未処理例外は `@RestControllerAdvice` でログし、クライアントには内部スタックを返さない。
- `JpaRepository` とSpecification/JPQLで検索を実装。N+1をテストする。
- DB接続プールはHikariCPを使用し、実負荷に合わせて設定する。
- 監査記録のトランザクション境界、通知イベントの確実な配送にはoutboxパターンも検討する。
