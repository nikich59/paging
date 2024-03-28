package uz.uzum.tezkor.data_paging

sealed interface PagedDataEdgeContent {

    data object Loading : PagedDataEdgeContent

    data object Error : PagedDataEdgeContent

    data object NoMoreContent : PagedDataEdgeContent
}
