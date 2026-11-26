/**
 * Simple client-side search and pagination utility for tables, cards, and grids.
 */
function initSearchPagination(options) {
    const settings = $.extend({
        searchInputId: '',          // ID of search input field (e.g. '#search-input')
        itemContainerSelector: '',   // Selector of container holding all items (e.g. '#table-body' or '#list-container')
        itemSelector: '',            // Selector of individual items (e.g. 'tr' or '.item-card')
        paginationContainerId: '',   // ID of container to append pagination control (e.g. '#pagination-container')
        pageSize: 5,                 // Number of items per page
        searchFields: [],            // Optional sub-elements/selectors to search inside. If empty, searches textContent.
        triggerBtnId: ''             // Optional button selector to trigger search instead of typing instantly
    }, options);

    const $searchInput = $(settings.searchInputId);
    const $itemContainer = $(settings.itemContainerSelector);
    const $paginationContainer = $(settings.paginationContainerId);

    if (!$itemContainer.length || !$searchInput.length || !$paginationContainer.length) {
        console.warn('SearchPagination initialization failed: missing required elements.', settings);
        return;
    }

    // Ensure the helper style to override bootstrap's d-flex display !important rule exists
    if (!$('#search-pagination-style').length) {
        $('<style id="search-pagination-style">.search-pagination-hidden { display: none !important; }</style>').appendTo('head');
    }

    let currentPage = 1;
    let visibleItems = [];

    function update() {
        const query = $searchInput.val() ? $searchInput.val().toLowerCase().trim() : '';
        const allItems = $itemContainer.find(settings.itemSelector);

        // 1. Filter items
        visibleItems = [];
        allItems.each(function() {
            const $item = $(this);
            let matches = false;

            if (!query) {
                matches = true;
            } else {
                if (settings.searchFields && settings.searchFields.length > 0) {
                    for (let field of settings.searchFields) {
                        let text = '';
                        if (field.startsWith('@')) {
                            // Search attribute value
                            text = $item.attr(field.substring(1)) || '';
                        } else {
                            // Search inner element text
                            text = $item.find(field).text() || '';
                        }
                        if (text.toLowerCase().includes(query)) {
                            matches = true;
                            break;
                        }
                    }
                } else {
                    // Search raw text of the item
                    if ($item.text().toLowerCase().includes(query)) {
                        matches = true;
                    }
                }
            }

            if (matches) {
                visibleItems.push($item[0]);
                $item.removeClass('search-pagination-hidden');
            } else {
                $item.addClass('search-pagination-hidden');
            }
        });

        // Toggle visibility of the "No items found" block or empty row
        let $emptyState = $itemContainer.find('.pagination-empty-message');
        if (visibleItems.length === 0) {
            if (!$emptyState.length) {
                if ($itemContainer.is('tbody')) {
                    $itemContainer.append('<tr class="pagination-empty-message"><td colspan="20" class="py-4 text-muted">No matching records found.</td></tr>');
                } else {
                    $itemContainer.append('<div class="col-12 text-center py-5 text-muted pagination-empty-message"><h3>No matching records found</h3><p>Try refining your keywords.</p></div>');
                }
            } else {
                $emptyState.removeClass('search-pagination-hidden');
            }
        } else {
            if ($emptyState.length) {
                $emptyState.addClass('search-pagination-hidden');
            }
        }

        // 2. Paginate items
        const totalItems = visibleItems.length;
        const totalPages = Math.max(1, Math.ceil(totalItems / settings.pageSize));

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        const startIndex = (currentPage - 1) * settings.pageSize;
        const endIndex = startIndex + settings.pageSize;

        // Hide all visible items, then show only the items for the current page
        $(visibleItems).addClass('search-pagination-hidden');
        $(visibleItems.slice(startIndex, endIndex)).removeClass('search-pagination-hidden');

        // 3. Render pagination controls
        renderPagination(totalPages);
    }

    function renderPagination(totalPages) {
        $paginationContainer.empty();
        if (totalPages <= 1) return; // Do not display if only one page

        const $ul = $('<ul class="pagination justify-content-center mt-4"></ul>');

        // Previous button
        const $prevLi = $('<li class="page-item"></li>');
        const $prevA = $('<a class="page-link" href="#" aria-label="Previous" style="color: #f96d00;"><span aria-hidden="true">&laquo;</span></a>');
        if (currentPage === 1) {
            $prevLi.addClass('disabled');
            $prevA.css('color', '#6c757d');
        } else {
            $prevA.click(function(e) {
                e.preventDefault();
                currentPage--;
                update();
            });
        }
        $prevLi.append($prevA).appendTo($ul);

        // Page numbers
        for (let i = 1; i <= totalPages; i++) {
            const $li = $('<li class="page-item"></li>');
            const $a = $('<a class="page-link" href="#" style="transition: all 0.3s;">' + i + '</a>');

            if (i === currentPage) {
                $li.addClass('active');
                $a.css({
                    'background-color': '#f96d00',
                    'border-color': '#f96d00',
                    'color': '#fff'
                });
            } else {
                $a.css('color', '#f96d00');
                $a.hover(
                    function() { $(this).css({'background-color': '#f96d00', 'color': '#fff', 'border-color': '#f96d00'}); },
                    function() { $(this).css({'background-color': '#fff', 'color': '#f96d00', 'border-color': '#dee2e6'}); }
                );
            }

            $a.click(function(e) {
                e.preventDefault();
                currentPage = i;
                update();
            });
            $li.append($a).appendTo($ul);
        }

        // Next button
        const $nextLi = $('<li class="page-item"></li>');
        const $nextA = $('<a class="page-link" href="#" aria-label="Next" style="color: #f96d00;"><span aria-hidden="true">&raquo;</span></a>');
        if (currentPage === totalPages) {
            $nextLi.addClass('disabled');
            $nextA.css('color', '#6c757d');
        } else {
            $nextA.click(function(e) {
                e.preventDefault();
                currentPage++;
                update();
            });
        }
        $nextLi.append($nextA).appendTo($ul);

        $paginationContainer.append($ul);
    }

    // Attach search event
    if (settings.triggerBtnId) {
        $(settings.triggerBtnId).on('click', function(e) {
            e.preventDefault();
            currentPage = 1;
            update();
        });
    } else {
        $searchInput.on('input', function() {
            currentPage = 1; // reset to page 1 on search key change
            update();
        });
    }

    // Initial draw
    update();
}
