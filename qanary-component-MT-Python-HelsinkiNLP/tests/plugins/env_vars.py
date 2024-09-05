import os
import pytest

@pytest.hookimpl(tryfirst=True)
def pytest_load_initial_conftests(args, early_config, parser):
    os.environ["SPRING_BOOT_ADMIN_URL"]="https://webengineering.ins.hs-anhalt.de:43740"
    os.environ["SPRING_BOOT_ADMIN_USERNAME"]="admin"
    os.environ["SPRING_BOOT_ADMIN_PASSWORD"]="admin"
    os.environ["SERVICE_HOST"]="http://webengineering.ins.hs-anhalt.de"
    os.environ["SERVER_PORT"]="41062"
    os.environ["SERVICE_NAME_COMPONENT"]="MT-Helsinki-NLP-Component"
    os.environ["SERVICE_DESCRIPTION_COMPONENT"]="MT tool that uses pre-trained models by Helsinki NLP implemented in transformers library"
