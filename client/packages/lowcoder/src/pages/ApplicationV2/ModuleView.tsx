import { useSelector } from "react-redux";
import { HomeLayout } from "./HomeLayout";
import { getUser } from "../../redux/selectors/usersSelectors";
import { MODULE_APPLICATIONS_URL } from "../../constants/routesURL";
import { normalAppListSelector } from "../../redux/selectors/applicationSelector";
import { AppTypeEnum } from "../../constants/applicationConstants";
import { trans } from "../../i18n";
import { Helmet } from "react-helmet";

export function ModuleView() {
  const user = useSelector(getUser);
  const allApplications = useSelector(normalAppListSelector);

  if (!user.currentOrgId) {
    return null;
  }

  return (
    <>
      <Helmet>{<title>{trans("productName")} {trans("home.modules")}</title>}</Helmet>
      <HomeLayout
        elements={allApplications.filter((a) => a.applicationType === AppTypeEnum.Module)}
        mode={"module"}
        breadcrumb={[{ text: trans("home.allModules"), path: MODULE_APPLICATIONS_URL }]} />
    </>
  );
}
