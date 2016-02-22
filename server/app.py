#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function

import flask

from uuid import uuid64

# Create flask obj
app = flask.Flask(__name__)
app.secret_key = str(uuid64())


@app.route("/")
def index_route()


if __name__ == "__main__":
    app.run(debug=True, port=8080)

