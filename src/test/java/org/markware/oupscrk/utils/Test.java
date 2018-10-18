package org.markware.oupscrk.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

public class Test {
    public static void main(String[] args) throws IOException, DataFormatException {
    	String test = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><meta name=\"google-site-verification\" content=\"EoXo80HtINVpGJQ5R7FQT-UhMzmvg6Lux-I_S6Wlhb8\"><title>mgreau.com</title><link rel=\"stylesheet\" href=\"/stylesheets/bootstrap.css\"><link rel=\"stylesheet\" href=\"/stylesheets/bootstrap-responsive.css\"><link rel=\"stylesheet\" href=\"/stylesheets/font-awesome.min.css\"><link rel=\"stylesheet\" href=\"/plugins/flexslider/flexslider.css\"><link rel=\"stylesheet\" href=\"/stylesheets/theme-style.css\"><link rel=\"stylesheet\" href=\"/stylesheets/alternative-colour.css\"><link rel=\"stylesheet\" href=\"/stylesheets/custom-style.css\"><link rel=\"stylesheet\" href=\"http://fonts.googleapis.com/css?family=Source+Sans+Pro:200,300,400,600,700,900,200italic,300italic,400italic,600italic,700italic,900italic\"><link rel=\"stylesheet\" href=\"http://fonts.googleapis.com/css?family=Condiment\"><script src=\"/plugins/retina/retina.js\"></script></head><body class=\"has-navbar-fixed-top page-index\"><div class=\"wrapper\" id=\"navigation\"><div class=\"navbar navbar-fixed-top\"><div class=\"navbar-inner\"><div class=\"container\">          <div class=\"brand\"><h1><a href=\"/\"><span class=\"em\">[site.title]</span></a></h1></div><a class=\"mobile-toggle-trigger\"><i class=\"icon-reorder\"></i></a><a class=\"mobile-toggle-trigger scroll-nav\" data-js=\"scroll-show\"><i class=\"icon-reorder\"></i></a><div class=\"mobile-toggle pull-right\"><ul class=\"nav\" id=\"main-menu\"><li class=\"\"><a href=\"/books.html\">Books</a></li><li class=\"\"><a href=\"/posts\">Blog</a></li><li class=\"\"><a href=\"/resume/resume.html\" target=\"mgreau-cv\">Work / Experience / Talks</a></li><li class=\"\"><a href=\"https://twitter.com/mgreau\" class=\"stamp\">@mgreau</a></li>              </ul><g:plusone size=\"medium\"></g:plusone><a href=\"https://twitter.com/share\" class=\"twitter-share-button\" data-lang=\"en\">Tweet</a></div></div></div></div></div><div id=\"content\"><section class=\"scroll-section about block primary\" id=\"about\"><div class=\"container\"><div class=\"row-fluid\"><div class=\"span3 photo\"><img src=\"https://1.gravatar.com/avatar/a81c764f137343d59a5e6264c2e3c1c2?s=278\" alt=\"Maxime's profile\" class=\"img-circle pull-center\" /></div><div class=\"span9 details\"><h2 class=\"primary-focus\">Maxime Gréau</h2><h3 class=\"secondary-focus\">Software Factory Manager @ eXo Platform</h3><p>• Speaker / Author / Technical Reviewer <br/>• Technology enthusiast: Java (EE), Docker, Git, Asciidoctor and Web.<br/>• Software Developer with over 13 years experience </p><div class=\"margin-top\"><a href=\"/resume/resume.html\" class=\"btn btn-large btn-primary-grad\">View my full resume <i class=\"icon-angle-down\"></i></a></div></div></div></div></section></div><footer id=\"footer\"><div class=\"container\"><div class=\"row-fluid pull-center\"><div id=\"contact\" class=\"social-media\"><a title=\"My Twitter account - @mgreau\" href=\"https://twitter.com/mgreau\"><i class=\"icomoon-twitter-3\"></i></a><a title=\"My G+ account\"  href=\"https://plus.google.com/u/0/110674805154532168093/posts/p/pub\"><i class=\"icomoon-google-plus-4\"></i></a><a title=\"My Github account - mgreau\"  href=\"https://github.com/mgreau\"><i class=\"icomoon-github-3\"></i></a><a title=\"My LinkedIn account\"  href=\"https://fr.linkedin.com/in/mgreau\"><i class=\"icomoon-linkedin\"></i></a><a href=\"http://www.javacodegeeks.com/\" imageanchor=\"1\" target=\"javacodegeek\"><img alt=\"Java Code Geeks\" src=\"http://cdn.javacodegeeks.com/wp-content/uploads/2012/12/JavaCodeGeek_Badge.png\" title=\"Java Code Geeks\" style=\"<background><white>;\"></white></background></a></div><p>Copyright 2013 © <a href=\"http://mgreau.com\">mgreau.com</a> | Baked by Awestruct - Asciidoc</p></div></div>  </footer><script type=\"text/javascript\"></script><script type=\"text/javascript\">\r\n" + 
    			"var _gaq = _gaq || [];\r\n" + 
    			"_gaq.push(['_setAccount','UA-20147445-1']);\r\n" + 
    			"_gaq.push(['_trackPageview']);\r\n" + 
    			"(function() {\r\n" + 
    			" var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;\r\n" + 
    			" ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';\r\n" + 
    			" var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);\r\n" + 
    			"})();\r\n" + 
    			"</script>\r\n" + 
    			"<script src=\"/javascripts/jquery.js\"></script><script src=\"/javascripts/bootstrap.min.js\"></script><script src=\"/plugins/flexslider/jquery.flexslider-min.js\"></script><script src=\"/plugins/jPanelMenu/jquery.jpanelmenu.min.js\"></script><script src=\"/plugins/jRespond/js/jRespond.js\"></script><script src=\"/plugins/onePageNav/jquery.scrollTo.js\"></script><script src=\"/plugins/onePageNav/jquery.nav.js\"></script><script src=\"/javascripts/script.js\"></script></body></html>";
    
    	byte[] encodedBody = CompressionUtils.encodeContentBody(test.getBytes("UTF-8"), "gzip");
    	
    	byte[] decodedBody = CompressionUtils.decodeContentBody(encodedBody, "gzip");
    	System.out.println(Arrays.toString(test.getBytes("UTF-8")));
    	System.out.println(Arrays.toString(decodedBody));
    	
    	System.out.println();
    	System.out.println();
    	
    	System.out.println(Arrays.toString(encodedBody));
    	System.out.println(Arrays.toString(CompressionUtils.encodeContentBody(decodedBody, "gzip")));
    	
    	System.out.println("Oupscrk test sdf klqsdf oupscrk oupscrK".replaceAll("[oO]upscrk", "oussama"));
    }
    
}
