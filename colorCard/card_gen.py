
"""
Generates color cards with rectangle or circle swatches
Python version: 3.7+
_________________________________________________________________________________________________

Requirements:

1. ppf.datamatrix (https://github.com/adrianschlatter/ppf.datamatrix/tree/master#installation)
2. sty (https://sty.mewo.dev/intro/install.html)
3. Inkscape should be installed for pdf / png export feature
   Note: Path to Inkscape.exe should be added to the Environment Variables
   Tutorial: https://helpdeskgeek.com/windows-10/add-windows-path-environment-variable/
_________________________________________________________________________________________________

Requires the color and value properties in the test parameter JSON file in the project
Example:

"values": [
        {
          "value": 0,
          "color": "#e91b75"
        },
        {
          "value": 0.5,
          "color": "#e64260"
        },
        ...
      ]
_________________________________________________________________________________________________

Usage:

> python card_gen.py <Parameter ID> <Sample color/value> [--pdf | --png]

_________________________________________________________________________________________________

Example commands:

Circle swatch card for specific test parameter:
> python card_gen.py WC-FM-F
> python card_gen.py WR-FM-pH
> python card_gen.py WC-FM-NO3

Circle swatch cards for all water test parameters:
> python card_gen.py WC

Rectangle swatch cards for all soil test parameters:
> python card_gen.py SR

Include a sample area color for a calibration point e.g. 0.5:
> python card_gen.py WC-FM-F 0.5

Include a sample area color with a specific color e.g. #ff4dd2:
> python card_gen.py WC-FM-F #ff4dd2

Export to pdf
> python card_gen.py WC-FM-F --pdf
> python card_gen.py WC --pdf
> python card_gen.py WR-FM-F #ff4dd2 --png

Export to png
> python card_gen.py WC-FM-F --png
> python card_gen.py WC --png
> python card_gen.py WR-FM-F 1.5 --png

_________________________________________________________________________________________________

Notes:

The generated color cards are saved in the CARD_FOLDER folder

_________________________________________________________________________________________________

"""


import sys
import json
import math
import enum
import logging
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
from ppf.datamatrix import DataMatrix
from decimal import Context, Decimal
from sty import fg, ef, rs

# Unit = mm
class RectangleCard:
    CARD_WIDTH = 130
    CARD_HEIGHT = 84.5
    QR_WIDTH = 7.5
    QR_HORIZONTAL_DISTANCE = 85
    QR_VERTICAL_DISTANCE = 54
    QR_STROKE_WIDTH = 1

    TOP_MARGIN = 4.5    
    TITLE_Y_POS = 7
    TITLE_FONT_SIZE = 3


class CircleCard:
    CARD_WIDTH = 83
    CARD_HEIGHT = 63
    QR_WIDTH = 5.1
    QR_HORIZONTAL_DISTANCE = 67.3
    QR_VERTICAL_DISTANCE = 45.1
    QR_STROKE_WIDTH = 0.7
    SWATCH_CIRCLE_DIAMETER = 39

    TOP_MARGIN = 2
    TITLE_Y_POS = 6.28
    TITLE_FONT_SIZE = 3

    CENTER_X = CARD_WIDTH / 2
    CENTER_Y = (CARD_HEIGHT / 2) + TOP_MARGIN
    RADIAN_DEGREE = math.pi / 180
    ANGLE_OFFSET = 90


JSON_FILE_PATH = ''
LOGO_PATH = 'images/logo.svg'
CARD_FOLDER = 'generated'


class SampleType(enum.Enum):
    Soil = 1
    Water = 2
    Compost = 3


class CardType(enum.Enum):
    Circle = 1
    Rectangle = 2


class ExportType(enum.Enum):
    none = ''
    Pdf = 'pdf'
    Png = 'png'

sample_type = SampleType.Soil
card_type = CardType.Rectangle
export_type = ExportType.none

error_msg = ''


def get_logo():
    with open(LOGO_PATH, "r") as f:
        logo = f.read()
    logo = logo.replace('<svg>', '').strip()
    logo = logo.replace('</svg>', '').strip()

    if card_type == CardType.Rectangle:
        matrix = "translate(0.17663044,-0.65413773)"
    else:
        matrix = "matrix(0.75094849,0,0,0.75094849,-6.6105598,0.80413316)"
    
    return f'\t<g transform="{matrix}">\n\t\t{logo}\n\t</g>\n'


def get_header_title(title):
    """Creates the title text"""
    
    if card_type == CardType.Rectangle:
        c = RectangleCard
    else:
        c = CircleCard
    
    x = c.QR_HORIZONTAL_DISTANCE
    x = x + (c.CARD_WIDTH - x) / 2
    return f'\t<text x="{x}" y="{c.TITLE_Y_POS}" text-anchor="end" style="font-family:Calibri;font-style:normal" font-size="{c.TITLE_FONT_SIZE}">{title}</text>\n'


def get_QR_corners():
        
    if card_type == CardType.Rectangle:
        c = RectangleCard
    else:
        c = CircleCard

    x = ((c.CARD_WIDTH - c.QR_HORIZONTAL_DISTANCE) / 2) + (c.QR_STROKE_WIDTH / 2)
    y = (((c.CARD_HEIGHT - c.QR_VERTICAL_DISTANCE) / 2) + (c.QR_STROKE_WIDTH / 2)) + c.TOP_MARGIN
    QR_SIZE = c.QR_WIDTH - c.QR_STROKE_WIDTH

    inner_rect_width = (c.QR_WIDTH * 2.2) / 5
    inner_rect_pos = (c.QR_WIDTH * 1.05) / 5

    svg = '\t<symbol id="qrc">\n' \
          + f'\t\t<rect  width="{QR_SIZE}" height="{QR_SIZE}" stroke="black" fill="#fff" stroke-width="{c.QR_STROKE_WIDTH}"/>\n' \
          + f'\t\t<rect  x="{inner_rect_pos}" y="{inner_rect_pos}" width="{inner_rect_width}" height="{inner_rect_width}" fill="#000"/>\n' \
          + '\t</symbol>\n' \
          + f'\t<use xlink:href="#qrc" x="{x}" y="{y}"/>\n' \
          + f'\t<use xlink:href="#qrc" x="{x + c.QR_HORIZONTAL_DISTANCE - c.QR_WIDTH}" y="{y}"/>\n' \
          + f'\t<use xlink:href="#qrc" x="{x}" y="{y + c.QR_VERTICAL_DISTANCE - c.QR_WIDTH}"/>\n' \
          + f'\t<use xlink:href="#qrc" x="{x + c.QR_HORIZONTAL_DISTANCE - c.QR_WIDTH}" y="{y + c.QR_VERTICAL_DISTANCE - c.QR_WIDTH}"/>\n'

    return svg


def get_data_matrix(parameter_id):
    """Creates the data matrix 2D code for the given parameter ID."""

    namespaces = {'n': 'http://www.w3.org/2000/svg'}
    data = parameter_id.replace('-', '')
    data_matrix = DataMatrix(data)
    svg = ET.fromstring(data_matrix.svg()).find(
        'n:path', namespaces).attrib['d']

    # Remove unnecessary move command at the end of the path
    remove_index = svg.rfind('m')
    if remove_index > -1:
        svg = svg[: remove_index]

    if card_type == CardType.Rectangle:
        transform_matrix = '1.5,0,0,1.5,88,33.25'
        if len(data) == 6:
            transform_matrix = '1.2857143,0,0,1.2857166,88.214286,33.464267'
        elif len(data) == 7:
            transform_matrix = '1.2857143,0,0,1.2857166,88.214286,33.464267'
    else:
        transform_matrix = '0.83333333,0,0,0.83332561,64.316668,27.66672'
        if len(data) == 6:
            transform_matrix = '0.71428571,0,0,0.7142469,64.435716,27.786025'
        elif len(data) == 7:
            transform_matrix = '0.71428571,0,0,0.7142887,64.435716,27.78569'

    return f'\t<g stroke="#000" stroke-width="1" transform="matrix({transform_matrix})">\n' \
           + f'\t\t<path d="{svg}" />\n' \
           + '\t</g>\n'


def get_rectangle_cuvette_area():
    """Creates the dotted line for the cuvette cutout"""

    return '\t<rect fill="none" style="stroke:#000;stroke-width:0.1;stroke-dasharray:0.8, 0.8;" width="16" height="54" x="56.681301" y="16.833235" />\n'


def get_rectangle_swatch():
    """Creates the 2 columns of swatches for the rectangle color card"""

    height = 54 / color_count
    svg = ''

    for i in range(color_count):
        svg += f'\t<text x="42.80323" y="{16.783234  + 1.2525 + (height/2) + height*i}" text-anchor="end" style="font-size:3.52777778px;">'
        svg += f'{calibration_points[i]["value"]}</text>\n'
        svg += f'\t<rect fill="{calibration_points[i]["color"]}" width="9" height="{height}" x="45.621677" y="{16.783234 + height*i}" />\n'

    for i in range(color_count - 1, -1, -1):
        svg += f'\t<rect fill="{calibration_points[abs(i - color_count + 1)]["color"]}" width="9" height="{height}" x="74.634193" y="{16.783234 + height*i}" />\n'

    svg += get_rectangle_cuvette_area()

    return svg


def get_circle_values():
    """The values to displayed next to each circle swatch sectors"""

    radius = CircleCard.SWATCH_CIRCLE_DIAMETER / 2 + 2
    slice_count = color_count * 2
    slice_angle = 360 / (slice_count)
    svg = f'\t<g transform="translate({CircleCard.CENTER_X},{CircleCard.CENTER_Y})" text-anchor="end" font-size="3" style="font-family:Calibri;font-style:normal">\n'
    for i in range(color_count, 0, -1):
        radian = CircleCard.RADIAN_DEGREE * (CircleCard.ANGLE_OFFSET + slice_angle*i - slice_angle/2)
        x = math.cos(radian) * radius
        y = -math.sin(radian) * radius

        # Slightly adjust position of first and last value
        if i == 1:
            x = x + 0.3
            y = y - 0.7
        if i == color_count:
            x = x + 1
            y = y + 0.7

        svg += f'\t\t<text x="{x}" y="{y + 1.2}">{calibration_points[i - 1]["value"]}</text>\n'
    svg += '\t</g>\n'
    return svg


def get_sector(x, y, diameter, a1, a2):
    """Creates the svg path for a sector of the pie"""

    cr = diameter / 2
    cx1 = math.cos(CircleCard.RADIAN_DEGREE * a2) * cr + x
    cy1 = -math.sin(CircleCard.RADIAN_DEGREE * a2) * cr + y
    cx2 = math.cos(CircleCard.RADIAN_DEGREE * a1) * cr + x
    cy2 = -math.sin(CircleCard.RADIAN_DEGREE * a1) * cr + y
    return f'M{x} {y} {cx1} {cy1} A{cr} {cr} 0 0 1 {cx2} {cy2}Z'


def get_circle_swatch():
    """Creates the circle with swatches"""

    slice_count = color_count * 2
    slice_angle = 360 / slice_count
    color_index = 0
    svg = f'\t<g transform="translate({CircleCard.CENTER_X},{CircleCard.CENTER_Y})">\n'
    for i in range(slice_count):
        angle = (slice_angle * i) + CircleCard.ANGLE_OFFSET
        svg += f'\t\t<path fill="{calibration_points[color_index]["color"]}" d="{get_sector(0, 0, CircleCard.SWATCH_CIRCLE_DIAMETER, angle, angle + slice_angle)}"/>\n'
        color_index += 1
        if color_index == color_count:
            color_index = 0
    svg += '\t</g>\n'

    svg += get_circle_values()

    return svg


def print_info(parameter_id, title, calibration_points, color_count):
    """Displays color and calibration point info in the console."""

    global error_msg

    print(ef.bold + '\n' + title + rs.bold_dim + ' : ' + parameter_id + '\n')
    try:
        for i in range(color_count):
            hex = calibration_points[i]["color"].lstrip('#')
            rgb = tuple(int(hex[i:i+2], 16) for i in (0, 2, 4))
            print(fg(rgb[0], rgb[1], rgb[2]) + '███████████' + fg.rs,
                  f' {calibration_points[i]["value"]} : {calibration_points[i]["color"]} {rgb}')
    except:
        error_msg = f'Error: {title} - Missing or invalid color info in json file'
        print(fg.red + error_msg + fg.rs)


def get_sample_color():
    """Creates a sample color area in the middle of the card for debug and testing."""

    svg = ''
    color = ''

    if len(sys.argv) > 2:
        input = sys.argv[2]
        if input.startswith("#"):
            color = input
        else:
            try:
                for i in range(color_count):
                    if Decimal(calibration_points[i]["value"]) == Decimal(input):
                        color = calibration_points[i]["color"]
                        break
            except:
                pass

    if color != '':
        if card_type == CardType.Circle:
            svg = f'\t<circle fill="{color}" cx="{CircleCard.CENTER_X}" cy="{CircleCard.CENTER_Y}" r="12"/>\n'
        else:
            svg = f'\t<path fill="{color}" d="m 57.903943,20.252348 c 5.329795,0.955429 10.122636,0.612361 13.563928,0.140317 m -10e-7,-0.09355 v 47.333431 c 0,0.984647 -0.792695,1.777342 -1.777343,1.777342 H 59.681284 c -0.984647,0 -1.777342,-0.792695 -1.777342,-1.777342 V 20.29912 c 2.669643,-1.221494 10.147163,-1.633446 13.563928,0 z" />\n'

    return svg


def parse_arguments():
    """Get the user input command line arguments"""

    global card_type
    global sample_type
    global export_type

    sample_type = SampleType.Soil
    if sys.argv[1].casefold().startswith('w'):
        sample_type = SampleType.Water
    elif sys.argv[1].casefold().startswith('c'):
        sample_type = SampleType.Compost

    card_type = CardType.Rectangle
    if sys.argv[1][1:2].casefold() == 'c':
        card_type = CardType.Circle

    if len(sys.argv) > 3:
        if sys.argv[3] == '--pdf':
            export_type = ExportType.Pdf
        elif sys.argv[3] == '--png':
            export_type = ExportType.Png

    if len(sys.argv) > 2:
        if sys.argv[2] == '--pdf':
            export_type = ExportType.Pdf
        elif sys.argv[2] == '--png':
            export_type = ExportType.Png


def save_and_export(card, parameter_id):
    """ Save to svg file and also export to other formats if requested"""

    folder = Path(CARD_FOLDER)
    folder.mkdir(exist_ok=True)
    f = open(f'{CARD_FOLDER}/{parameter_id}.svg', 'w')
    f.write(card)
    f.close()

    if export_type != ExportType.none:
        subprocess.call(
            f'"inkscape" {CARD_FOLDER}/{parameter_id}.svg --export-filename={CARD_FOLDER}/{parameter_id}.{export_type.value} --export-dpi=300 > nul 2>&1', shell=True)


if len(sys.argv) < 2:
    print(fg.red + 'Error: Parameter ID not provided' + fg.rs)
else:

    parse_arguments()

    # Load the appropriate tests json file
    c = CircleCard
    data = json.load(open(JSON_FILE_PATH + 'parameters.json'))

    input_id = sys.argv[1].casefold().replace('-', '')
    title = ''

    for parameter in data['customer1']['water_card']['tests']:

        parameter_id = parameter['uuid'].casefold().replace('-', '')

        if len(input_id) > 2:
            if parameter_id != input_id:
                continue
        elif len(input_id) == 2:
            if (sample_type == SampleType.Water and not parameter_id.startswith('w')) or \
                    (sample_type == SampleType.Soil and not parameter_id.startswith('s')) or \
                    (sample_type == SampleType.Compost and not parameter_id.startswith('c')):
                continue

        title = parameter['name']

        try:
            parameter_id = parameter['uuid']
            if 'results' in parameter and 'values' in parameter['results'][0]:
                calibration_points = parameter['results'][0]['values']
            else:
                continue

            color_count = len(calibration_points)

            card = f'<svg width="{c.CARD_WIDTH}mm" height="{c.CARD_HEIGHT}mm" viewBox="0 0 {c.CARD_WIDTH} {c.CARD_HEIGHT}" ' \
                   + 'font-family="Bahnschrift" shape-rendering="geometricPrecision"\n' \
                   + '\t' + 'xmlns="http://www.w3.org/2000/svg"\n\t' + 'xmlns:xlink="http://www.w3.org/1999/xlink">\n' \
                   + '\t<rect width="100%" height="100%" fill="white"/>\n'

            card += get_logo()

            card += get_header_title(title)

            card += get_QR_corners()

            card += get_data_matrix(parameter_id)

            if card_type == CardType.Rectangle:
                card += get_rectangle_swatch()
            else:
                card += get_circle_swatch()

            card += get_sample_color()

            card += '</svg>\n'

            print_info(parameter_id, title, calibration_points, color_count)

            save_and_export(card, parameter_id)

        except Exception as e:
            print_info(parameter_id, title, calibration_points, color_count)
            logging.error(e, exc_info=True)

        print('________________________________________________________\n')

    if title == '':
        print(fg.red + 'Error: Invalid parameter ID' + fg.rs)

if error_msg != '':
    print('\n' + fg.red + ef.bold + error_msg + rs.bold_dim + fg.rs + '\n')
