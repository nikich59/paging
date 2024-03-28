package uz.uzum.tezkor.data_paging

fun interface PagedDataExceptionHandler {
    fun onException(exception: Exception)
}
