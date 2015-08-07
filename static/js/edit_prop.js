$(function() {
	$('.task_headings').click(function(){
		$('.task_headings').not(this).each(function() {
			$(this).children('.panel').slideUp();
		});
		$(this).children('.panel').slideDown();
	});
	
	$('#edit_prop_btn').click(function() {
		$('#edit_prop_form').show();
	});
	$('#edit_done_btn').click(function() {
		$('#edit_prop_form').css('display','none');
	});
});