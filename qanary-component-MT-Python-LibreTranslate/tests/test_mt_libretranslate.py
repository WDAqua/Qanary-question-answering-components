from component.mt_libretranslate import *
from component import app
from unittest.mock import patch
import re
from unittest import TestCase


class TestComponent(TestCase):

    logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

    questions = list([{"uri": "urn:test-uri", "text": "test_input"}])
    endpoint = "urn:qanary#test-endpoint"
    in_graph = "urn:qanary#test-inGraph"
    out_graph = "urn:qanary#test-outGraph"

    source_language = "en"

    test_translation_placeholder = "test_translation"

    request_data = '''{
        "values": {
            "urn:qanary#endpoint": "urn:qanary#test-endpoint",
            "urn:qanary#inGraph": "urn:qanary#test-inGraph",
            "urn:qanary#outGraph": "urn:qanary#test-outGraph"
        },
        "endpoint": "urn:qanary#test-endpoint",
        "inGraph": "urn:qanary#test-inGraph",
        "outGrpah": "urn:qanary#test-outGraph"
    }'''

    headers = {
        "Content-Type": "application/json"
    }


    def test_qanary_service(self):

        with app.test_client() as client, \
                patch('component.mt_libretranslate.get_text_question_in_graph') as mocked_get_text_question_in_graph, \
                patch('component.mt_libretranslate.insert_into_triplestore') as mocked_insert_into_triplestore, \
                patch('component.mt_libretranslate.translate_input') as mocked_translate_input:

            # given a non-english question is present in the current graph
            mocked_get_text_question_in_graph.return_value = self.questions
            mocked_insert_into_triplestore.return_value = None
            mocked_translate_input.return_value = self.test_translation_placeholder, None

            # when a call to /annotatequestion is made
            response_json = client.post("/annotatequestion", headers = self.headers, data = self.request_data)

            # then
            # the text question is retrieved from the triplestore
            mocked_get_text_question_in_graph.assert_called_with(triplestore_endpoint=self.endpoint, graph=self.in_graph)

            # new information is pushed to the triplestore 
            mocked_insert_into_triplestore.assert_called()

            args = mocked_insert_into_triplestore.call_args.args
            query_stored = re.sub(r"(\\n\W*|\n\W*)", " ", args[1])

            # the source language is correctly identified and annotated
            self.assertRegex(query_stored, r".*AnnotationOfQuestionLanguage(.*;\W?)*oa:hasBody \""+self.source_language+r"\".*\.")
            # the question is translated and the result is annotated
            assert self.test_translation_placeholder in query_stored.lower()

            # the response is not empty
            assert response_json != None

