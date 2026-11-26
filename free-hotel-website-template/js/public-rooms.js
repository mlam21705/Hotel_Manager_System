(function($) {
    "use strict";

    // Helper function to format currency as Vietnamese Dong (VND)
    function formatVND(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    // Helper to get query parameters
    function getQueryParam(param) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    }

    // Load rooms list (either featured first 4, or all rooms)
    function loadRoomsList(containerId, limit = null) {
        const $container = $(`#${containerId}`);
        if ($container.length === 0) return;

        // Clear existing static items
        $container.empty();

        $.ajax({
            url: '/api/public/room-types',
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                if (!data || data.length === 0) {
                    $container.html('<div class="col-md-12 text-center py-5"><h3>No room types found.</h3></div>');
                    return;
                }

                // Slice data if limit is specified
                const rooms = limit ? data.slice(0, limit) : data;

                rooms.forEach((room, index) => {
                    // Determine image layout classes to match the design system
                    const isImageRight = Math.floor(index / 2) % 2 === 1;
                    const imageOrderClass = isImageRight ? 'order-md-last' : '';
                    const arrowClass = isImageRight ? 'right-arrow' : 'left-arrow';

                    // Get primary image or fallback to default
                    let imageUrl = 'images/room-1.jpg';
                    if (room.images && room.images.length > 0) {
                        const primaryImg = room.images.find(img => img.isPrimary);
                        if (primaryImg) {
                            imageUrl = primaryImg.imageUrl;
                        } else {
                            imageUrl = room.images[0].imageUrl;
                        }
                    }

                    const priceFormatted = formatVND(room.basePrice);

                    const cardHtml = `
                        <div class="col-lg-6 ftco-animate fadeInUp ftco-animated">
                            <div class="room-wrap d-md-flex">
                                <a href="rooms-single.html?id=${room.roomTypeId}" class="img ${imageOrderClass}" style="background-image: url(${imageUrl});"></a>
                                <div class="half ${arrowClass} d-flex align-items-center">
                                    <div class="text p-4 text-center">
                                        <p class="star mb-0">
                                            <span class="ion-ios-star"></span>
                                            <span class="ion-ios-star"></span>
                                            <span class="ion-ios-star"></span>
                                            <span class="ion-ios-star"></span>
                                            <span class="ion-ios-star"></span>
                                        </p>
                                        <p class="mb-0"><span class="price mr-1">${priceFormatted}</span> <span class="per">per night</span></p>
                                        <h3 class="mb-3"><a href="rooms-single.html?id=${room.roomTypeId}">${room.typeName}</a></h3>
                                        <p class="pt-1"><a href="rooms-single.html?id=${room.roomTypeId}" class="btn-custom px-3 py-2 rounded">View Details <span class="icon-long-arrow-right"></span></a></p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;

                    $container.append(cardHtml);
                });

                // Re-trigger scroll animations for dynamically added elements
                if (typeof $.fn.waypoint === 'function') {
                    setTimeout(function() {
                        $('.ftco-animate').each(function() {
                            var el = $(this);
                            if (!el.hasClass('ftco-animated')) {
                                el.addClass('fadeInUp ftco-animated');
                            }
                        });
                    }, 100);
                }
            },
            error: function(xhr, status, error) {
                console.error('Failed to load room types:', error);
                $container.html('<div class="col-md-12 text-center py-5"><h3>Failed to load rooms. Please try again later.</h3></div>');
            }
        });
    }

    // Load single room type details
    function loadRoomDetails() {
        const $title = $('#room-detail-title');
        const $desc = $('#room-detail-desc');
        if ($title.length === 0) return; // Not on single room details page

        const roomTypeId = getQueryParam('id');
        if (!roomTypeId) {
            window.location.href = 'rooms.html';
            return;
        }

        $.ajax({
            url: `/api/public/room-types/${roomTypeId}`,
            method: 'GET',
            dataType: 'json',
            success: function(room) {
                // Update elements
                $title.text(room.typeName);
                $desc.text(room.description || 'No description available.');
                $('#room-detail-breadcrumb').text(room.typeName);

                // Update details specs list
                const specsHtml = `
                    <li><span>Max:</span> ${room.capacity} Persons</li>
                    <li><span>Size:</span> 45 m2</li>
                    <li><span>Price:</span> ${formatVND(room.basePrice)}</li>
                    <li><span>Bed:</span> ${room.capacity > 2 ? 2 : 1}</li>
                `;
                $('#room-detail-specs').html(specsHtml);

                // Re-build image slider carousel
                const $slider = $('.single-slider');
                if ($slider.length > 0) {
                    // Destroy if already initialized
                    if ($slider.data('owl.carousel')) {
                        $slider.owlCarousel('destroy');
                    }

                    $slider.empty();

                    if (room.images && room.images.length > 0) {
                        room.images.forEach(img => {
                            $slider.append(`
                                <div class="item">
                                    <div class="room-img" style="background-image: url(${img.imageUrl});"></div>
                                </div>
                            `);
                        });
                    } else {
                        // Default image if none exist
                        $slider.append(`
                            <div class="item">
                                    <div class="room-img" style="background-image: url(images/room-1.jpg);"></div>
                            </div>
                        `);
                    }

                    // Re-initialize Owl Carousel
                    $slider.owlCarousel({
                        animateOut: 'fadeOut',
                        animateIn: 'fadeIn',
                        autoplay: true,
                        loop: true,
                        items: 1,
                        margin: 0,
                        stagePadding: 0,
                        nav: true,
                        dots: true,
                        navText: ['<span class="ion-ios-arrow-back">', '<span class="ion-ios-arrow-forward">'],
                        responsive: {
                            0: { items: 1 },
                            600: { items: 1 },
                            1000: { items: 1 }
                        }
                    });
                }

                // Handle fade animations
                setTimeout(function() {
                    $('.ftco-animate').each(function() {
                        var el = $(this);
                        if (!el.hasClass('ftco-animated')) {
                            el.addClass('fadeInUp ftco-animated');
                        }
                    });
                }, 100);
            },
            error: function(xhr, status, error) {
                console.error('Failed to load room details:', error);
                window.location.href = 'rooms.html';
            }
        });
    }

    $(document).ready(function() {
        loadRoomsList('featured-rooms-container', 4);
        loadRoomsList('rooms-container');
        loadRoomDetails();
    });

})(jQuery);
