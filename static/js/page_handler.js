$(function() {
	$('.panel').click(function(){
		$(this).children('.panel-body').slideDown();
		$('.panel').not(this).each(function() {
			$(this).children('.panel-body').slideUp();
		});
	});
	
	$('select#files').change(function() {
		var selected_child = $(this).children(':selected')[0];
		var index = $("option").index(selected_child);
		window.location.href = window.location.href.replace(/[/]\d/, 
														'/' + index.toString());
	});
	
	$('#edit_prop_btn').click(function() {
		$('#edit_prop_form').show();
	});
	$('#edit_done_btn').click(function() {
		$('#edit_prop_form').css('display','none');
	});
	
	$('#main_tab').click(function() {
		$('.active').removeClass('active');
		$('#main_tab').addClass('active');
		
		$('.active_content').css("display", "none");
		$('.active_content').removeClass('active_content');
		$('#amend_access').addClass('active_content');
		$('#amend_access').css("display", "block");
	});
	$('#dict_tab').click(function() {
		$('.active').removeClass('active');
		$('#dict_tab').addClass('active');
		
		$('.active_content').css("display", "none");
		$('.active_content').removeClass('active_content');
		$('#dict').addClass('active_content');
		$('#dict').css("display", "block");
	});
	$('#tag_tab').click(function() {
		$('.active').removeClass('active');
		$('#tag_tab').addClass('active');
		
		$('.active_content').css("display", "none");
		$('.active_content').removeClass('active_content');
		$('#tag').addClass('active_content');
		$('#tag').css("display", "block");
	});
});