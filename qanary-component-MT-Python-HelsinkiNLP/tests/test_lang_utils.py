import logging
from unittest import mock
from unittest import TestCase
import os
import importlib

class TestLangUtils(TestCase):

    logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)

    @mock.patch.dict(os.environ, {'SOURCE_LANGUAGE': 'fr'})
    def test_only_one_source_language(self):
        import utils.lang_utils
        importlib.reload(utils.lang_utils)
        from utils.lang_utils import translation_options
        assert 'fr' in translation_options.keys()
        assert len(translation_options.keys()) == 1


    @mock.patch.dict(os.environ, {'TARGET_LANGUAGE': 'ru'})
    def test_only_one_target_language(self):
        import utils.lang_utils
        importlib.reload(utils.lang_utils)
        from utils.lang_utils import translation_options
        # currently, there are only two source languages that support target language 'ru'
        assert len(translation_options.items()) == 2
        assert ('en', ['ru']) in translation_options.items()
        assert ('fr', ['ru']) in translation_options.items()


    @mock.patch.dict(os.environ, {'SOURCE_LANGUAGE': 'en', 'TARGET_LANGUAGE': 'es'})
    def test_specific_source_and_target_language(self):
        import utils.lang_utils
        importlib.reload(utils.lang_utils)
        from utils.lang_utils import translation_options
        assert translation_options == {'en': ['es']}


    @mock.patch.dict(os.environ, {'SOURCE_LANGUAGE': 'zh'})
    def test_unsupported_source_language_raises_error(self):
        try:
            import utils.lang_utils
            importlib.reload(utils.lang_utils)
        except ValueError as ve:
            logging.error(ve)
            pass


    @mock.patch.dict(os.environ, {'SOURCE_LANGUAGE': 'en', 'TARGET_LANGUAGE': 'zh'})
    def test_unsupported_target_for_source_language_raises_error(self):
        try:
            import utils.lang_utils
            importlib.reload(utils.lang_utils)
        except ValueError as ve:
            logging.error(ve)
            pass


    @mock.patch.dict(os.environ, {'TARGET_LANGUAGE': 'zh'})
    def test_unsupported_target_language_raises_error(self):
        try:
            import utils.lang_utils
            importlib.reload(utils.lang_utils)
        except ValueError as ve:
            logging.error(ve)
            pass
