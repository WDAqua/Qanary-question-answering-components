import logging
from component import app
from fastapi.testclient import TestClient
from unittest.mock import patch
from unittest import mock
import re
from unittest import TestCase
from qanary_helpers.language_queries import QuestionTextWithLanguage
import os
import importlib


class TestComponent(TestCase):

    logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

    questions = list([{"uri": "urn:test-uri", "text": "Was ist die Hauptstadt von Deutschland?"}])
    endpoint = "urn:qanary#test-endpoint"
    in_graph = "urn:qanary#test-inGraph"
    out_graph = "urn:qanary#test-outGraph"

    source_language = "de"
    target_language = "en"

    source_texts = [
        QuestionTextWithLanguage("uri", "Was ist die Hauptstadt von Deutschland?", "de")
    ]

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

    client = TestClient(app)

    @mock.patch.dict(os.environ, {'SOURCE_LANGUAGE': 'de', 'TARGET_LANGUAGE': 'en'})
    def test_qanary_service(self):
        import utils.lang_utils
        importlib.reload(utils.lang_utils)
        import component.mt_nllb
        importlib.reload(component.mt_nllb)
        from component import app

        logging.info("port: %s" % (os.environ["SERVER_PORT"]))
        assert os.environ["SERVICE_NAME_COMPONENT"] == "MT-NLLB-Component"
        assert os.environ["SOURCE_LANGUAGE"] == self.source_language
        assert os.environ["TARGET_LANGUAGE"] == self.target_language

        with patch('component.mt_nllb.get_text_question_in_graph') as mocked_get_text_question_in_graph, \
             patch('component.mt_nllb.find_source_texts_in_triplestore') as mocked_find_source_texts_in_triplestore, \
             patch('component.mt_nllb.insert_into_triplestore') as mocked_insert_into_triplestore:

            # given a non-english question is present in the current graph
            mocked_get_text_question_in_graph.return_value = self.questions
            mocked_find_source_texts_in_triplestore.return_value = self.source_texts
            mocked_insert_into_triplestore.return_value = None

            # when a call to /annotatequestion is made
            response_json = self.client.post("/annotatequestion", headers = self.headers, data = self.request_data)

            # then the text question is retrieved from the triplestore
            mocked_get_text_question_in_graph.assert_called_with(triplestore_endpoint=self.endpoint, graph=self.in_graph)

            mocked_find_source_texts_in_triplestore.assert_called_with(triplestore_endpoint=self.endpoint, graph_uri=self.in_graph, lang=self.source_language)
            assert mocked_find_source_texts_in_triplestore.call_count == 1

            # get arguments of the (2) separate insert calls 
            arg_list = mocked_insert_into_triplestore.call_args_list
            # get the call arguments for question translation
            call_args_translation = [a.args for a in arg_list if "AnnotationOfQuestionTranslation" in a.args[1]]
            assert len(call_args_translation) == 1

            # clean query strings
            query_translation = re.sub(r"(\\n\W*|\n\W*)", " ", call_args_translation[0][1])

            # then the triplestore is updated twice 
            # (question language and translation)
            assert mocked_insert_into_triplestore.call_count == 1

            # then the question is translated and the result is annotated
            self.assertRegex(query_translation, r".*AnnotationOfQuestionTranslation(.*;\W?)*oa:hasBody \".*\"@" + self.target_language + r".*\.")
            assert "@"+self.target_language in query_translation.lower()

            # then the response is not empty
            assert response_json != None


    # test with all supported languages enabled
    def test_translate_input(self):
        import component.mt_nllb
        from component.mt_nllb import translate_input
        import utils.lang_utils
        importlib.reload(utils.lang_utils)
        importlib.reload(component.mt_nllb)
        translations = [
            {"text": "Was ist die Hauptstadt von Deutschland?",
             "translation": "What is the capital of Germany?",
             "source_lang": "de", "target_lang": "en"},
            {"text": "What is the capital of Germany?",
             "translation": "Quelle est la capitale de l'Allemagne?",
             "source_lang": "en", "target_lang": "fr"},
#            {"text": "What is the capital of Germany?", TODO: result from NLLB: Какова столица Германии?
#             "translation": "Какая столица Германии?",
#             "source_lang": "en", "target_lang": "ru"},
        ]

        for translation in translations:
            expected = translation["translation"]
            actual = translate_input(translation["text"], translation["source_lang"], translation["target_lang"])
            assert expected == actual
