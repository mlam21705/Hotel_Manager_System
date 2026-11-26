$(function() {
  // Load header
  var $header = $('#header-container');
  if ($header.length) {
    $header.load('header.html', function() {
      // Highlight active nav item based on current page URL
      var path = window.location.pathname;
      var page = path.split("/").pop();
      
      // Remove any existing active classes
      $('#ftco-nav .nav-item').removeClass('active');
      
      if (page === "" || page === "index.html") {
        $('#ftco-nav a[href="index.html"]').parent().addClass('active');
      } else if (page === "rooms.html" || page === "rooms-single.html") {
        $('#ftco-nav a[href="rooms.html"]').parent().addClass('active');
      } else if (page === "restaurant.html") {
        $('#ftco-nav a[href="restaurant.html"]').parent().addClass('active');
      } else if (page === "about.html") {
        $('#ftco-nav a[href="about.html"]').parent().addClass('active');
      } else if (page === "blog.html" || page === "blog-single.html") {
        $('#ftco-nav a[href="blog.html"]').parent().addClass('active');
      } else if (page === "contact.html") {
        $('#ftco-nav a[href="contact.html"]').parent().addClass('active');
      }
      
      // Dispatch event for auth.js to handle login/logout button injection
      document.dispatchEvent(new Event('navbarLoaded'));
    });
  }

  // Load footer
  var $footer = $('#footer-container');
  if ($footer.length) {
    $footer.load('footer.html');
  }
});
