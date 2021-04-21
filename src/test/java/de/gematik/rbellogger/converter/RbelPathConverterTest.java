/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.*;
import org.junit.jupiter.api.Test;

public class RbelPathConverterTest {

    @Test
    public void convertAbsolutePathWithQueryParameters() {
        final RbelElement rbelElement = RbelLogger.build().getRbelConverter().convertMessage("/foobar?"
            + "&nonce=997755"
            + "&client_id=fdsafds"
            + "&code_challenge=Ca3Ve8jSsBQOBFVqQvLs1E-dGV1BXg2FTvrd-Tg19Vg");

        assertThat(rbelElement)
            .isInstanceOf(RbelPathElement.class);
        assertThat(((RbelPathElement) rbelElement).getBasicPath().getContent())
            .isEqualTo("/foobar");
        assertThat(((RbelPathElement) rbelElement).getQueryParameter().getElementMap())
            .containsOnlyKeys("nonce", "client_id", "code_challenge");
    }

    @Test
    public void domainAndProtocolAndQuery_shouldConvertNested() {
        final RbelElement rbelElement = RbelLogger.build().getRbelConverter().convertMessage(
            "http://redirect.gematik.de/erezept/token?code=eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwiZXhwIjoxNjE0NDE2MTUxfQ..yo1fe7ZrDMhZIz7Z.cWGs95g2mOeVGkqzQXQo9EsTMAOtJbEBfGzqPWIGrWwOoQ_EibAlC7ypku6tuCTYQcGvG4BTdlBT_j1OsKrgDgZXaW2SCw-AwunD4pWkKKXK1xMmF1HSdLEf_Vr6yENq6eamxAdNBE8J6guDy4MCogeBe8TtmXr451Jvg3VRMUMQShWYGkjr5iVj0hfmQXsQGnFUnBPH460Ai-m9K8vQdDHa-4eUa407I8DQJLQRCbvgPFIBzTBYvLZYl9942qlsC_TFoObuzic_1UO0lGRa01JRjAgOVOOYFO_ZCr3bSCX2VRa9mnRhnECLITwTZnt1vCW3umXOANNNNYI-4o0NIHUet3Tz2Qget8ZjXRjM8XljesrQyYOBDu6zX-katT3U8LRtf1jS5oz7mpcbCjSSkiM25QteqLdY2h9mmoEPcV0ZTs8HN4OEQASPBYkyXFHtEPdTzMvONqIMNn3tG3xQAk_u1wGfapoYS31_P8jmZoePCmJR0xBTQxHpWXF_-kxBz2RPran8RahBmJP-sucuTGVxLrQ2pLH8cj0l20CeS10ax8D0aXUVy-FEh_qLHc65VjCzHjNZNdIDJug6mmd8vHn3d_Y0g-LNHqTA2WfE3vIvsRX8YAStwjNhyi-Iz90GTlkoEpaCg8xEqhANvkUgz1hDGVuEU9hCEBHJsll-hoCFnTSHrTzZkbQK3ccMNSCeANctepESAzlc_8MhtpJ6tiDBoHq7o2R4Lm6rI6Vb5CEpP8ExuFFX6jxA2_U_Sr49N0bdiF4LUG-kbk6A9GkkHobjhBB6UJqgekuYWb3zC-x66MIVPE-VQ0yvBCeFw0qerHNtIi0MmWkxPT2I9xm3dHf01WM639DC4mNjVG2ZpIxMFkyGWmg1GeRnyZHFCAM7KSMfVZlJ7Ixlc4kXQuvoHu-X04SJvbw4SYxdKzSjUGoHsEPLnN1fKGweWMtiE84vb1Lmypj7g5uzp2otlaJUGv2nYwMEDzlT3dNNLQeFIyyNmwccPGM1RKOkdV3xuktq-2qPEJGhuxMAFdgFT0sptXSxAqpu5ExAZFBABQLA_mqyQkgC7PFhGewe_tvjVgVDT0Py_ne8pVAnscEatllut0MzvR2ZuiOZb6al44nYgYGNzqnUW3ZEOrfH52hp3mtYDNss8jxGI5kO9MR0SrnEyFzeC1MtoHm4FAkW9R44KJwMNPdDsDsW9jrGYiluaTDxZuyq9VVLmbP-zbID2-kA65A2k-1tfLu0MO8Piv14neccyHWO82j40QjDzK53vAa81vLFHM03N68XtV0WYq3dUZVhMcUGVAfGxEiV-cHbfUFZys_EguthrxdhUpYCULzDnNvEzx16haI6JA.1t26fHLOfI3-kGOYW1fEQg&ssotoken=eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwiZXhwIjoxNjE0NDU5MjkxfQ..oiOpvJRM-mnVD-O0.b1IoflRyiAHpuxmRzJ8pa20WJh_cG5XN8u6ttIPFAOcYgrSDQtnOAbVbe4pmA9ZhGDJrdrxqKKyF-fGEI5pGhYdLlR5GEkQ8Z6DHBdD_K_Ojt-MYW6OAzoa4_831HH-dzlrfvinelL7giqIsTOzs2541yu0Wr7vokLQd9FbFmbx1o6jVBvS0tzkJCrGJ-y2ys-EFoXWjDs-iFYTho5RdUHk1RZqSiLdV39gZ9XvVhXztlfIJuTnWHumjYwXUnGXsZoub3oXbXH9QpqRqCws4-YtF_gtgJ-o9m81mBlfpiehk3XzJH2SWm2QqacI7sh5Vc151pYSTUV-cTq-23yjvK866j8Cq9Q2lF9iuGZKiTwsa9NwSSvItGXVKsh8CtIc_iaPEq4R9hQ_8SuVkV-riXe-AK8SEnjYgRSCwlEJ4jo3sxsISuX98d405CNRYjcj0q0tkfaSfDyKjEOCmKQ3X-pD0qcLOUOBHIEvqGiwb4d-Ierk0E5L1Zhu7SfRqQze89bvpns8ByqQjtiaoKEypHbnZsMiEjHDN2yjhNKDu7ZHdKdidd4DGKvWmetGIAIZKU7_FD7oR7K_PnSoHiUxJHdTCixYy-V9T-4IuArLXaBBO_2_JMqJvg9LxTEQ_NvHXJQT-liKRXDfoEX8N6-iEfINOjLKxAMeff1XuZ10JOiKkqRYNHYRGtQaxPwOpSHX4P1ZZ0iAJK5F8GLJnC8oXVNwWjZaiyh6y00EP56IHjs6Fgag-6C8C5uJiWvkxpXw5tblUEtxEBl-tx9iR3tmCX6ZogW82i6XWc3Ot09JzSfxqbHcb2N1_80khGxUMqJJMitSdw-28FZk9AG3K5NAcu5hYGZThMdbF_yDKEoEJqi7UO-gUCgDl0HQWHmA9LvS8bQ8nPnXi9zp3j2IxYn7_mQL-iYjrjN5NJsRS4yEg7PZahgPOn32NS27XJhVj74SN4R--wtOxngi9q5lG4YXeji6hvrr73sTzMQlQaKXO0uzyX_C6CuO5hv-P7k2SyWbGvpqZLDJbWA-zQeEdXwnvhG9Um3tvVaNun6PtP1_fNhALOoO96XqLXxjwouzJfNGXlcc2R1wL4G0cnEQCqDT8f6Sj2uW0Tk0Ev5IiUJg5Swcc_c3X7pbimDHmnBPT30rKWEB4Rh5JZkiYWo_KnWOXboItnD_l4wCnyyP_N7MUEfuIYBmpbdSLFI5qJQ93s_m1EQaF-8HE2iUF3UugJVeenAR3tbTlFz1LIzZPtzdB2_8IhvexWsGptaFWQUAo4xV8bPp6-BKCHI9e8TpbvsPgQwuur98gGND7EBxngZDgtp9wPToBKvycKguszYQqtRQTtr8VfJ7a9dwUoAWGE67k_7DHDJYmHwn5n_RtJfveym6lXE2BHi7a3OeVBp6T3HpNXcs1-4EUan80ae5GgNHTAvixEdyGjZ80nJ-Xl6EZax23QHV-dC_SCJ26O6ixP_CDLTQehZHkfeDYe4piRH-t3iukv7Dpy65haxjnhoalcnT3Ue2Y8uK4vSqU0y4EZ7kjjgvKgbPCGbG9KF4Ob2QPSJQ7C9WdCryKCMJ3xoXAIj-vXfFbYWNVcjtxCYpjky3sMBP0dgCRn1MpmdpQGKb59h1l6w_UGgs1gQHM9OSK1tUnLnooSxYSJp5zKYmoFxGCh3mr601JytjkGeDogOP8ay-E4Qai9TNQkYeIKkGQmidvWXQxhMALSDTwi78RhlAvRQ0kckBxzOCwLP5ie8V1bwrM1330sLp3DBg8yBjgwvmBqZGywGhstd9ttpHcJRsSY_KFk9D8IRjDYNBjs6KsOysSByWaREAc4XZalLAfp0CQyCUlVP9mZ23YLJCCDerzqzMtbeG8MjABx_zwHOPxsLvDVdIuK6nAbvnKfrJnBwHMsxfkiB7tCLS1ZrdCg0pmK_PRm8rQwI0uW-qqBr3Wv7-9pt6_ZDou4rMu_vqxTRG3W4nsvBOFbWSJgD2IV4NXOXePtDA_j6heRaIGjjvWNwFI_R8SwbZcPoP7jBgH6vhmwkSc6mVgDMa8sRrZP99iyAfPjuQF5_Up2QF9ashHEVbvLEB25fD1EDIHng1JToneiDTJvMUuBoKinBbviSkvzULg_QcpSFQPDOiFm4Vaw3T__9wH-LMdzEgO-xpj64csIArnBn5CjjXbXt2CZcRY9ignJjtH81bDSAB_8Ml_M94r_RRqKiGsdHKd9tUroW48XWVrbU_MrRgdwMZh5yi3bNZmuYy7-tYr7oKNAFMCatpLzK00wXx6M-D7wtA-KlOgOaIxrZrfZBZ2u-AuJcrBmKbNOldq1DjpRYb-38-RqUt6U-PFVaXbnfp9y5zPt7lF7ir8i16MMuWxOZc8HUcWBsBme_AdZUB5A0LpUWjpenpVEGPQL6LPsjXXxSHFTS56jK8ZueNS9Z0GJoW8Q0xcztPKsKyHZod52zepCiTtalZDZskRNTWFvRIUA_wMZCaX46kkEhjfAeu1xlJ276KWEGOucPG_WjAZX1KNd-sGk5ATkhhhKjnqQtuTf6rnldzjYNxvHckvrx-q1-nxSlubrMyCeBC6-JIvNCq--HpqoOzEethnxObLkXmETZL51fS82ZYi8Jd418T3FT0dkppBQIKTza_vKrn-0ym-F70BQRv8jxM6kMc7lGaWIWmR5B6OLKHed1GcxV4P9Nzrf1oizApsQ75ku7Zqj0911rCNTUp40flf3L3w455q8kCS2LwydAk_xlkhBd6d4KPIyZ9F_O58bQcjBVi3QFywWW38Q_-as14zOn9bUYj_y6NgW-jMPqQ_YUy_q1Lobz8PTPztoJhsHbR_Ik6JFyjtZ5y1voKq46Q2ND3uefBCh6AEF9yxEbJGG5CcyvC6mYigoSE7lNwGTfTAIcRD55vf_7kaX3XqQJoDNLFtNfXyqZBj1fBjHZmn8d2A79mwfQPdTjZkcvGCsYpSW6zkOIKAMO166J-SdhZ6Qg8EheC_8Oa7335wBYjXhqOvQxsg6p15FPWRdPrbK_v5pfGiS_90IjKTaueyexS0fDgyGdcrX91MB4inpo5rRz5_4B5z-HShvxqe8DqW2A3YsoqIzsHK2bu8DuIaEwF9lZODUP8cmUAJhZvxMIKD5Q.Z9pkn81i_q-hlPgxGlG2ow&state=xxxstatexxx1a");

        assertThat(rbelElement)
            .isInstanceOf(RbelPathElement.class);
        assertThat(((RbelPathElement) rbelElement).getBasicPath().getContent())
            .isEqualTo("http://redirect.gematik.de/erezept/token");
        assertThat(((RbelPathElement) rbelElement).getQueryParameter().getChildKeys())
            .containsOnly("code", "ssotoken", "state");
        final RbelElement code = ((RbelPathElement) rbelElement).getQueryParameter().getFirst("code").get();
        assertThat(code)
            .isInstanceOf(RbelJweElement.class);
        assertThat(rbelElement.traverseAndReturnNestedMembers())
            .containsKey("code");
    }

    @Test
    public void urlEscapedParameterValues_shouldContainOriginalContent() {
        final RbelElement rbelElement = RbelLogger.build().getRbelConverter().convertMessage(
            "/sign_response?scope=pairing%20openid&response_type=code&code_challenge_method=S256&redirect_uri=http%3A%2F%2Fredirect.gematik.de%2Ferezept&state=Hl6TLS1LjhFs3AUy&nonce=np3RhlBn1Epw5fhRc9v6&client_id=eRezeptApp&code_challenge=aQswmTcEbHi6TDrSSq9MgfpqyixHcbe581MbaFlYkpU");

        final RbelElement scope = ((RbelPathElement) rbelElement).getQueryParameter().getFirst("scope").get();
        assertThat(scope.getRawMessage())
            .isEqualTo("scope=pairing%20openid");
        assertThat(scope.getContent())
            .isEqualTo("pairing openid");
    }

    @Test
    public void simpleUrlWithoutQuery() {
        final RbelElement rbelElement = RbelLogger.build().getRbelConverter()
            .convertMessage("http://redirect.gematik.de/foo/bar");

        assertThat(rbelElement)
            .isInstanceOf(RbelPathElement.class);
        assertThat(((RbelPathElement) rbelElement).getBasicPath().getContent())
            .isEqualTo("http://redirect.gematik.de/foo/bar");
        assertThat(((RbelPathElement) rbelElement).getQueryParameter().getChildElements())
            .isEmpty();
    }
}
