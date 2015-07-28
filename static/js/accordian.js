$(function(){
	$('.task_headings').click(function(){
		$('.task_headings').not(this).each(function() {
			$(this).children('.panel').slideUp();
		});
		$(this).children('.panel').slideDown();
	});
});