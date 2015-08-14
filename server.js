var express = require('express');
var app = express();
var path = require('path');
var swig  = require('swig');
var multer  = require('multer');
var child_process = require('child_process');

var upload = multer({ dest: 'uploads' });
var files = [], filenames = [], outputs = []; 

app.use('/static', express.static('static'));
app.use('/uploads', express.static('uploads'));
app.engine('html', swig.renderFile);

app.route('/')
	.get(function (req, res) {
		var home_html = path.join(__dirname + '/home.html');
		res.end(swig.renderFile(home_html));
	})
	.post(upload.array('fileitem'), function (req, res) {
		files = req.files;
		filenames = files.map(function (file) {
			return file['originalname'];
		});
		var i = 0;
		function callback(output) {
			var output_str = output.trim().split("\n");
			var len = output_str.length;
			for (var i=0; i < len; i++) {
				outputs.push(output_str[i]);
			}
			console.log(outputs);
			res.redirect('/edit/0');
			/**i++;
			if (i < files.length) {
				run_cmd();
			} else {
				res.redirect('/edit/0');
			}	*/
		}
		function run_cmd() {
			//files[i]['path']
			var paths = "";
			var len = files.length;
			paths += files[0]['path'];
			for (var i = 1; i < len; i++) {
				paths += " ";
				paths += files[i]['path'];
			}
			console.log(paths);
			var cmd = 'java -cp target/accessibility-0.0.1-SNAPSHOT.jar check/Checker ' + paths;
			child_process.exec(cmd,
			function (err, stdout, stderr) {
				if (err) {
					console.log('exec error: ' + err);
				} else {
					callback(stdout);
				}
			});
		}
		run_cmd();
	});

app.get('/about', function (req, res) {
	var about_html = path.join(__dirname + '/about.html');
	res.end(swig.renderFile(about_html));
});

app.get('/download/:id', function (req, res) {
	var id = req.params.id;
	res.download(path.join(__dirname + '/' + files[id]['path']),
					files[id]['originalname']);
});

app.route('/edit/:id')
	.get(function (req, res) {
		var id = parseInt(req.params.id);
		var doc_data = {
			filenames: filenames,
			curr_file_index: id,
			output: JSON.parse(outputs[id])
		};
		console.log(doc_data.output);
		var edit_html = path.join(__dirname + '/edit.html');
		res.end(swig.renderFile("edit.html", {data:doc_data}));
	});

var server = app.listen(3000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});