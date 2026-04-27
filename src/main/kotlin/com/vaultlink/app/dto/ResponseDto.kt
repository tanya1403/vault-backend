data class CursorResponse<T>(
    val content: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
)
data class MarkVaultRequest(
    val documentIds: List<String>,
    val vaultingDate: String? = null
)
data class DocumentDTO(
    val id: String,
    val name: String,
    val category: String,
    val subCategory: String,
    val label: String?,
    val type: String?,
    val status: String?,
    val createdDate: String,
    val sentToKleeto: String,
    val physicalCopy: String,
    val scannedCopy: String,
    val certifiedCopy: String,
    val lodName: String,
    val vaultingDate: String?
)
data class BranchDTO(
    val branchId: String,
    val branchName: String,
    val address: String?,
    val csmName: String?,
    val mobile: String?,
    val totalDocuments: Int
)
data class LaiDTO(
    val lai: String,
    val customerName: String?,
    val totalFiles: Int,
)
data class ResponseDto(
    val isSuccess: Boolean,
    val message: String
)
