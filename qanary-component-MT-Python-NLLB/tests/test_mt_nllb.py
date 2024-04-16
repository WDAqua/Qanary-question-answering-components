from component.mt_nllb import *
from component import app
from unittest.mock import patch
import re
from unittest import TestCase


class TestComponent(TestCase):

    logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

    questions = list([{"uri": "urn:test-uri", "text": "was ist ein Test?"}])
    endpoint = "urn:qanary#test-endpoint"
    in_graph = "urn:qanary#test-inGraph"
    out_graph = "urn:qanary#test-outGraph"

    source_language = "de"
    target_language = "en"

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
                patch('component.mt_nllb.get_text_question_in_graph') as mocked_get_text_question_in_graph, \
                patch('component.mt_nllb.insert_into_triplestore') as mocked_insert_into_triplestore:

            # given a non-english question is present in the current graph
            mocked_get_text_question_in_graph.return_value = self.questions
            mocked_insert_into_triplestore.return_value = None

            # when a call to /annotatequestion is made
            response_json = client.post("/annotatequestion", headers = self.headers, data = self.request_data)

            # then the text question is retrieved from the triplestore
            mocked_get_text_question_in_graph.assert_called_with(triplestore_endpoint=self.endpoint, graph=self.in_graph)

            # get arguments of the (2) separate insert calls 
            arg_list = mocked_insert_into_triplestore.call_args_list
            # get the call arguments for question translation
            call_args_translation = [a.args for a in arg_list if "AnnotationOfQuestionTranslation" in a.args[1]]
            assert len(call_args_translation) == 1
            # get the call arguments for question language
            call_args_language = [a.args for a in arg_list if "AnnotationOfQuestionLanguage" in a.args[1]]
            assert len(call_args_language) == 1

            # clean query strings
            query_translation = re.sub(r"(\\n\W*|\n\W*)", " ", call_args_translation[0][1])
            query_language = re.sub(r"(\\n\W*|\n\W*)", " ", call_args_language[0][1])

            # then the triplestore is updated twice 
            # (question language and translation)
            assert mocked_insert_into_triplestore.call_count == 2

            # then the source language is correctly identified and annotated
            self.assertRegex(query_language, r".*AnnotationOfQuestionLanguage(.*;\W?)*oa:hasBody \""+self.source_language+r"\".*\.")

            # then the question is translated and the result is annotated
            self.assertRegex(query_translation, r".*AnnotationOfQuestionTranslation(.*;\W?)*oa:hasBody \".*\"@" + self.target_language + r".*\.")
            assert "@"+self.target_language in query_translation.lower()

            # then the response is not empty
            assert response_json != None
