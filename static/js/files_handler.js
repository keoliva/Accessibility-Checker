var DOC_HANDLER = {
	onReady: function() {
		$("#uploaded_files").change(DOC_HANDLER.update_file_info);
		$("#submit_button").click(DOC_HANDLER.submit_file);
	},
	update_file_info: function() {
		var files = $(this)[0].files;
		var length = files["length"];
		var txt = "";
		for (var i=0; i < length; i++) {
			file = files[i];
			if (file.type != "application/pdf") {
				delete files[i];
				txt = txt + "<li class='list-group-item'><strike>" + file.name + "</strike></li>";
			} else {
				txt = txt + "<li class='list-group-item'>" + files[i].name + "</li>"
			}
			//$('#file_name').val(file.name);
			
			//var url = URL.createObjectURL(file);
			//$("#file_data").val(url);
			/*var reader = new FileReader();
			reader.onloadend = function () {
				var arr = new Uint32Array(reader.result)
				var arg = JSON.stringify(arr);
				console.log(arg.length);
				$("#file_data").val(arg);
			}
			reader.readAsArrayBuffer(file);*/
		}
		$('#files_info').html(txt);
	},
	submit_file: function() {
		var files = $("#uploaded_files")[0].files;
		if (files["length"] == 0) {
			alert("You have no file selected.");
			return false;
		} else {
			document.forms[0].submit();
		}
	}
};

$(document).ready(DOC_HANDLER.onReady);